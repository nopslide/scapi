/**
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
* Copyright (c) 2012 - SCAPI (http://crypto.biu.ac.il/scapi)
* This file is part of the SCAPI project.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
* to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
* FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* We request that any publication and/or code referring to and/or based on SCAPI contain an appropriate citation to SCAPI, including a reference to
* http://crypto.biu.ac.il/SCAPI.
* 
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
*/
package edu.biu.scapi.interactiveMidProtocols.ot.semiHonest;

import java.security.SecureRandom;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.interactiveMidProtocols.ot.OTROnByteArrayOutput;
import edu.biu.scapi.interactiveMidProtocols.ot.OTROutput;
import edu.biu.scapi.interactiveMidProtocols.ot.OTSMessage;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;

/**
 * Concrete class for Semi-Honest OT assuming DDH receiver ON BYTE ARRAY.
 * This class derived from OTReceiverDDHSemiHonestAbs and implements the functionality 
 * related to the byte array inputs.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class OTReceiverOnByteArraySemiHonest extends OTReceiverDDHSemiHonestAbs{
	private KeyDerivationFunction kdf; //Used in the calculation.
	
	/**
	 * Constructor that gets the channel and chooses default values of DlogGroup and SecureRandom.
	 */
	public OTReceiverOnByteArraySemiHonest(Channel channel){
		super(channel);
	}
	
	/**
	 * Constructor that sets the given channel, dlogGroup, kdf and random.
	 * @param channel
	 * @param dlog must be DDH secure.
	 * @param kdf
	 * @param random
	 */
	public OTReceiverOnByteArraySemiHonest(Channel channel, DlogGroup dlog, KeyDerivationFunction kdf, SecureRandom random){
		
		super(channel, dlog, random);
		this.kdf = kdf;
	}

	/**
	 * Runs the following lines from the protocol:
	 * "COMPUTE kσ = (u)^alpha						
	 *	OUTPUT  xσ = vσ XOR KDF(|cσ|,kσ)"	
	 * @param message received from the sender. must be OTSOnByteArraySemiHonestMessage.
	 * @return OTROutput contains Xσ
	 */
	protected OTROutput computeFinalXSigma(OTSMessage message) {
		//If message is not instance of OTSOnByteArraySemiHonestMessage, throw Exception.
		if(!(message instanceof OTSOnByteArraySemiHonestMessage)){
			throw new IllegalArgumentException("message should be instance of OTSOnByteArraySemiHonestMessage");
		}
		
		OTSOnByteArraySemiHonestMessage msg = (OTSOnByteArraySemiHonestMessage)message;
		
		//Compute kσ:
		GroupElement u = dlog.reconstructElement(true, msg.getU());
		GroupElement kSigma = dlog.exponentiate(u, alpha);
		byte[] kBytes = dlog.mapAnyGroupElementToByteArray(kSigma);
		
		//Get v0 or v1 according to σ
		byte[] vSigma = null;
		if (sigma == 0){
			vSigma = msg.getV0();
		} 
		if (sigma == 1) {
			vSigma = msg.getV1();
		}
		
		//Compute kdf result:
		int len = vSigma.length;
		byte[] xSigma = kdf.deriveKey(kBytes, 0, kBytes.length, len).getEncoded();
		
		//Xores the result from the kdf with vSigma.
		for(int i=0; i<len; i++){
			xSigma[i] = (byte) (vSigma[i] ^ xSigma[i]);
		}
		
		//Create and return the output containing xσ
		return new OTROnByteArrayOutput(xSigma);
	}
	
	
}