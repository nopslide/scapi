package edu.biu.scapi.primitives.universalHash;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.exceptions.UnInitializedException;
import edu.biu.scapi.paddings.BitPadding;
import edu.biu.scapi.paddings.NoPadding;
import edu.biu.scapi.paddings.PaddingParameterSpec;
import edu.biu.scapi.paddings.PaddingScheme;
import edu.biu.scapi.tools.Factories.PaddingFactory;

/** 
 * Concrete class of perfect universal hash for evaluation hash function.
 * 
* @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Meital Levy)
 */
public final class EvaluationHashFunction extends UniversalHashAbs {
	
	protected long evalHashPtr; // pointer to the native evaluation object
	private PaddingScheme padding;
	//native functions. These functions are implemented in the NTLJavaInterface dll using the JNI
	
	//creates the native object and initializes it with the secret key
	private native long initHash(byte[] key, long keyOffset);
	//computes the evaluation hash function
	//we don't send the input offset because we always send the padded array which the offset is always 0 
	private native void computeFunction(long evalHashPtr, byte[] in, byte[] out, int outOffset);
	
	
	
	public void init(SecretKey secretKey) {

		//passes the key to the native function, which creates a native evaluation hash function instance.
		//the return value is the pointer to this instance, which we set to the class member evalHashPtr
		evalHashPtr = initHash(secretKey.getEncoded(), 0);
		
		//sets the key
		super.init(secretKey);
		
		padding = new BitPadding();
	}
	
	public void init(SecretKey secretKey, AlgorithmParameterSpec params) throws FactoriesException {
		
		//passes the key to the native function, which creates a native evaluation hash function instance.
		//the return value is the pointer to this instance, which we set to the class member evalHashPtr
		evalHashPtr = initHash(secretKey.getEncoded(), 0);
		
		if (params instanceof PaddingParameterSpec){
			padding = PaddingFactory.getInstance().getObject(((PaddingParameterSpec) params).getPaddingName());
		} else {
			padding = new BitPadding();
		}
		
		//sets the parameters
		super.init(secretKey, params);	
	}
	
	/**
	 * Evaluation hash function can get any input size which is between 0 to 64t bits. while t = 2^24.
	 * @return the upper bound of the input size - 64t
	 */
	public int getInputSize() {
		//limit = t = 2^24
		int limit = (int) Math.pow(2, 24);
		//limit = 8t, which is 64t bits in bytes
		limit = limit * 8;
		//save maximum 8 byte to the padding
		limit = limit - 8;
		return limit;
	}

	/** 
	 * @return the output size of evaluation hash function - 8 bytes.
	 */
	public int getOutputSize() {
		
		//64 bits long
		return 8;
	}

	/**
	 * @return the algorithm name - Evaluation Hash Function
	 */
	public String getAlgorithmName() {
		
		return "Evaluation Hash Function";
		
	}

	
	public void compute(byte[] in, int inOffset, int inLen, byte[] out,
			int outOffset) throws UnInitializedException, IllegalBlockSizeException {
		if (!isInitialized()){
			throw new UnInitializedException();
		}
		//checks that the offset and length are correct
		if ((inOffset > in.length) || (inOffset+inLen> in.length)){
			throw new ArrayIndexOutOfBoundsException("wrong offset for the given input buffer");
		}
		if ((outOffset > out.length) || (outOffset+getOutputSize() > out.length)){
			throw new ArrayIndexOutOfBoundsException("wrong offset for the given output buffer");
		}
		
		//checks that the input length is not greater than the upper limit
		if(inLen > getInputSize()){
			throw new IllegalBlockSizeException("input length must be less than 64*(2^24-1) bits long");
		}
		
		byte[] paddedArray = null;
		//pad the input.
		if ((inLen%8) == 0){
			//the input is aligned to 64 bits so pads it as aligned array
			paddedArray = pad(in, inOffset, inLen, 8);
		} else {
			if (padding instanceof NoPadding){
				throw new IllegalArgumentException("input is not aligned to blockSize");
			}
			//gets the number of bytes to add in order to get an aligned array
			int inputSizeMod8 = inLen % 8;
			int leftToAlign = 8 - inputSizeMod8;
			//the input is not aligned to 64 bits so pads it to aligned array
			paddedArray = pad(in, inOffset, inLen, leftToAlign);
		}
		
		//calls the native function compute on the padded array.
		computeFunction(evalHashPtr, paddedArray, out, outOffset);
	}
	
	/**
	 * This padding is used to get an array aligned to 8 bytes (64 bits).
	 * The padding is done by calling the padding scheme class member to pad the array.
	 * The input for this function is an array of size that is not aligned to 8 bytes.
	 * @param input the input to pad. 
	 * @param offset the offset to take the input bytes from
	 * @param length the length of the input. This length is not aligned to 8 bytes.
	 * @return the aligned array
	 */
	private byte[] pad(byte[] input, int offset, int length, int padSize){

		//copy the relevant part of the array to a new one
		byte[] inputToPad = new byte[length];
		System.arraycopy(input, offset, inputToPad, 0, length);
		
		//call the padding scheme to pad the array
		return padding.pad(inputToPad, padSize);
	}
	
	
	static {
		 
		 //load the NTL jni dll
		 System.loadLibrary("NTLJavaInterface");
	}
}