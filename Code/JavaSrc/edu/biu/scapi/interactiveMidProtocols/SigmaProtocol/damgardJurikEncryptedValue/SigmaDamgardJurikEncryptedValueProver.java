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
package edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.damgardJurikEncryptedValue;

import java.math.BigInteger;
import java.security.SecureRandom;

import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.DJBasedSigma;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.SigmaProverComputation;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.SigmaSimulator;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.damgardJurikEncryptedZero.SigmaDJEncryptedZeroProverInput;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.damgardJurikEncryptedZero.SigmaDamgardJurikEncryptedZeroProver;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaProtocolInput;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaProtocolMsg;
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.DamgardJurikPublicKey;
import edu.biu.scapi.midLayer.ciphertext.BigIntegerCiphertext;
import edu.biu.scapi.midLayer.plaintext.BigIntegerPlainText;

/**
 * Concrete implementation of Sigma Protocol prover computation.<p>
 * 
 * This protocol is used for a party who encrypted a value x to prove that it indeed encrypted x.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class SigmaDamgardJurikEncryptedValueProver implements SigmaProverComputation, DJBasedSigma{

	/*	
	  This class uses an instance of SigmaDamgardJurikEncryptedZeroProver with:
	  	�	Common input: (n,c�) where c�=c*(1+n)^(-x) mod N'
		�	P�s private input: a value r <- Zq such that c�=r^N mod N�.
	*/	 
	
	private SigmaDamgardJurikEncryptedZeroProver sigmaDamgardJurik;	//underlying SigmaDamgardJurikProver to use.
	private int lengthParameter;									// length parameter in BITS.
	
	/**
	 * Constructor that gets the soundness parameter, length parameter and SecureRandom.
	 * @param t Soundness parameter in BITS.
	 * @param lengthParameter length parameter in BITS.
	 * @param random
	 */
	public SigmaDamgardJurikEncryptedValueProver(int t, int lengthParameter, SecureRandom random) {
		
		//Creates the underlying sigmaDamgardJurik object with the given parameters.
		sigmaDamgardJurik = new SigmaDamgardJurikEncryptedZeroProver(t, lengthParameter, random);
		this.lengthParameter = lengthParameter;
	}
	
	/**
	 * Default constructor that chooses default values for the parameters.
	 */
	public SigmaDamgardJurikEncryptedValueProver() {
		lengthParameter = 1;
		//Creates the underlying sigmaDamgardJurik object with default parameters.
		sigmaDamgardJurik = new SigmaDamgardJurikEncryptedZeroProver(80, lengthParameter, new SecureRandom());
		
	}

	/**
	 * Returns the soundness parameter for this Sigma protocol.
	 * @return t soundness parameter
	 */
	public int getSoundness(){
		//Delegates the computation to the underlying sigmaDamgardJurik prover.
		return sigmaDamgardJurik.getSoundness();
	}
	
	/**
	 * Converts the input to the underlying object input.
	 * @param input MUST be an instance of SigmaDJEncryptedValueProverInput.
	 * @throws IllegalArgumentException if input is not an instance of SigmaDJEncryptedValueProverInput.
	 */
	public void setInput(SigmaProtocolInput in) {
		
		/*
		 * Converts the input (n, c, x, r) to (n, c', r) where c� = c*(1+n)^(-x) mod N'.
		 */
		if (!(in instanceof SigmaDJEncryptedValueProverInput)){
			throw new IllegalArgumentException("the given input must be an instance of SigmaDJEncryptedValueProverInput");
		}
		SigmaDJEncryptedValueProverInput input = (SigmaDJEncryptedValueProverInput) in;
		
		//Get public key, cipher and plaintext.
		DamgardJurikPublicKey pubKey = input.getPublicKey();
		BigIntegerPlainText plaintext = input.getPlaintext();
		BigIntegerCiphertext cipher = input.getCiphertext();
		
		//Convert the cipher c to c' = c*(1+n)^(-x)
		BigInteger n = pubKey.getModulus();
		BigInteger nPlusOne = n.add(BigInteger.ONE);
		
		//calculate N' = n^(s+1).
		BigInteger NTag = n.pow(lengthParameter + 1);
		
		//Calculate (n+1)^(-x)
		BigInteger minusX = plaintext.getX().negate();
		BigInteger multVal = nPlusOne.modPow(minusX, NTag);
		
		//Calculate the ciphertext for DamgardJurikEncryptedZero - c*(n+1)^(-x).
		BigInteger newCipher = cipher.getCipher().multiply(multVal).mod(NTag);
		BigIntegerCiphertext cipherTag = new BigIntegerCiphertext(newCipher);
		
		//Create an input object to the underlying sigmaDamgardJurik prover.
		SigmaDJEncryptedZeroProverInput underlyingInput = new SigmaDJEncryptedZeroProverInput(pubKey, cipherTag, input.getR());
		sigmaDamgardJurik.setInput(underlyingInput);
		
	}

	/**
	 * Samples random value r in Zq.
	 */
	public void sampleRandomValues() {
		//Delegates to the underlying sigmaDamgardJurik prover.
		sigmaDamgardJurik.sampleRandomValues();
	}

	/**
	 * Computes the first message of the protocol.
	 * @return the computed message
	 */
	public SigmaProtocolMsg computeFirstMsg() {
		//Delegates the computation to the underlying sigmaDamgardJurik prover.
		return sigmaDamgardJurik.computeFirstMsg();
	}

	/**
	 * Computes the second message of the protocol.
	 * @param challenge
	 * @return the computed message.
	 * @throws CheatAttemptException if the received challenge's length is not equal to the soundness parameter.
	 */
	public SigmaProtocolMsg computeSecondMsg(byte[] challenge) throws CheatAttemptException {
		//Delegates the computation to the underlying sigmaDamgardJurik prover.
		return sigmaDamgardJurik.computeSecondMsg(challenge);
		
	}
	
	/**
	 * Returns the simulator that matches this sigma protocol prover.
	 * @return SigmaDamgardJurikEncryptedValueSimulator
	 */
	public SigmaSimulator getSimulator(){
		return new SigmaDamgardJurikEncryptedValueSimulator(sigmaDamgardJurik.getSimulator());
	}

}
