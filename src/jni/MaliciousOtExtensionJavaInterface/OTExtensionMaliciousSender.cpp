#include "OTExtensionMaliciousSender.h"

/*
 * Function initOtSender : This function initializes the sender object 
 * and creates the connection with the receiver
 * 
 * param ipAddress : The ip address of the sender computer for connection
 * param port : The port to be used for sending/receiving data over the network
 * returns : A pointer to the receiver object that was created and later be used to run the protcol
 */
JNIEXPORT jlong JNICALL Java_edu_biu_scapi_interactiveMidProtocols_ot_otBatch_otExtension_OTExtensionMaliciousSender_initOtSender(JNIEnv *env, jobject, jstring ipAddress, jint port, 
jint koblitzOrZpSize, jint numOfThreads) {

  // globals that must be set:
  // m_bUseECC = true;
  // m_sSecLvl = LT;
  // m_nPID = 0; // role, 0 for sender, 1 for receiver
  // BYTE version = C_OT;//Choose OT extension version: G_OT, C_OT or R_OT
  // m_nNumOTThreads = 1;
  // m_nChecks = 380; //Number of checks between the base-OTs


  // deprecated

  // //use ECC koblitz
  // if(koblitzOrZpSize==163 || koblitzOrZpSize==233 || koblitzOrZpSize==283){

  //   m_bUseECC = true;
  //   //The security parameter (163,233,283 for ECC or 1024, 2048, 3072 for FFC)
  //   m_nSecParam = koblitzOrZpSize;
  // }
  // //use Zp
  // else if(koblitzOrZpSize==1024 || koblitzOrZpSize==2048 || koblitzOrZpSize==3072) {

  //   m_bUseECC = false;
  //   //The security parameter (163,233,283 for ECC or 1024, 2048, 3072 for FFC)
  //   m_nSecParam = koblitzOrZpSize;
  // }

  // get the ip address from java
  const char* address = env->GetStringUTFChars(ipAddress, NULL);
  
  return (jlong) InitOTSender(address, (int) port, (int) nbaseots, (int) numOTs);
}

// --------------------------------------------------------------------------------

/*
 * Function runOtAsSender : This function runs the ot extension as the sender.
 * 
 * param x1 : The input array that holds all the x1,i for each ot in a one 
 * dimensional array one element after the other
 * param x2 : The input array that holds all the x2,i for each ot in a one 
 * dimensional array one element after the other
 * param bitLength : The length of each element
 */
JNIEXPORT void JNICALL Java_edu_biu_scapi_interactiveMidProtocols_ot_otBatch_otExtension_OTExtensionMaliciousSender_runOtAsSender(JNIEnv *env, jobject, jlong sender, jbyteArray x1, jbyteArray x2, jbyteArray deltaFromJava, jint numOfOts, jint bitLength, jstring version) {


  // The masking function with which the values that are sent 
  // in the last communication step are processed
  // Choose OT extension version: G_OT, C_OT or R_OT
  BYTE ver;
  
  // get ot version from java
  const char* str = env->GetStringUTFChars(version, NULL);

  // (supports all of the SHA hashes. 
  // Get the name of the required hash and instantiate that hash.)
  if(strcmp (str,"general") == 0) {
    ver = G_OT;
  } else if(strcmp (str,"correlated") == 0) {
    ver = C_OT;
  } else if(strcmp (str,"random") == 0) {
    ver = R_OT;
  }

  Mal_OTExtensionSender * otsender = (Mal_OTExtensionSender *) sender;
  jbyte * x1Arr = env->GetByteArrayElements(x1, 0);
  jbyte * x2Arr = env->GetByteArrayElements(x2, 0);
  jbyte * deltaArr;
  
  CBitVector delta, X1, X2;
  //Create X1 and X2 as two arrays with "numOTs" entries of "bitlength" bit-values
  X1.Create(numOfOts, bitLength);
  X2.Create(numOfOts, bitLength);


  // general ot ----------------------------------------------------------------

  if(ver ==G_OT){
    //copy the values given from java
    for(int i = 0; i < numOfOts*bitLength/8; i++)
      {
	X1.SetByte(i, x1Arr[i]);
	X2.SetByte(i, x2Arr[i]);			
      }
  }

  // correlated ot -------------------------------------------------------------
  else if(ver == C_OT){
    //get the delta from java
    deltaArr = env->GetByteArrayElements(deltaFromJava, 0);
    m_fMaskFct = new XORMasking(bitLength);
    delta.Create(numOfOts, bitLength);

    // set the delta values given from java
    int deltaSizeInBytes = numOfOts * bitLength / 8;
    for(int i = 0; i < deltaSizeInBytes; i++) {
      delta.SetByte(i, deltaArr[i]);
    }

    //creates delta as an array with "numOTs" entries of "bitlength" 
    // bit-values and fills delta with random values
    //delta.Create(numOfOts, bitLength, m_aSeed, m_nCounter);
  }

  // random ot -----------------------------------------------------------------
  else if(ver==R_OT){
    //no need to set any values. There is no input for x0 and x1 and no input for delta
  }
	
  //run the ot extension as the sender
  ObliviouslySend((OTExtensionSender*) sender, X1, X2, numOfOts, bitLength, ver, delta);

  if(ver != G_OT){ //we need to copy x0 and x1 

    //get the values from the ot and copy them to x1Arr, x2Arr wich later on will be copied to the java values x1 and x2
    for(int i = 0; i < numOfOts*bitLength/8; i++) {
      //copy each byte result to out
      x1Arr[i] = X1.GetByte(i);
      x2Arr[i] = X2.GetByte(i);  
    }

    if(ver==C_OT) {
      env->ReleaseByteArrayElements(deltaFromJava,deltaArr,0);
      delete m_fMaskFct;
    }
  }

  //make sure to release the memory created in c++. The JVM will not release it automatically.
  env->ReleaseByteArrayElements(x1,x1Arr,0);
  env->ReleaseByteArrayElements(x2,x2Arr,0);

  X1.delCBitVector();
  X2.delCBitVector();
  delta.delCBitVector();
}

/*
 * Function deleteSender : deletes the sender object
 * param sender: a pointer to the sender object.
 */
JNIEXPORT void JNICALL Java_edu_biu_scapi_interactiveMidProtocols_ot_otBatch_otExtension_OTExtensionMaliciousSender_deleteSender(JNIEnv * env, jobject, jlong sender) {

  Cleanup();
  delete (Mal_OTExtensionSender*) sender;
}