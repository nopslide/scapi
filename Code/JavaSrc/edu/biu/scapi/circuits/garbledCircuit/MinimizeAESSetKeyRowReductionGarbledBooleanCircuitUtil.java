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
package edu.biu.scapi.circuits.garbledCircuit;

import java.security.SecureRandom;
import java.util.BitSet;

import edu.biu.scapi.circuits.circuit.BooleanCircuit;
import edu.biu.scapi.circuits.circuit.Gate;
import edu.biu.scapi.circuits.encryption.AES128MultiKeyEncryption;
import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;
import edu.biu.scapi.primitives.kdf.bc.BcKdfISO18033;
import edu.biu.scapi.primitives.prf.AES;
import edu.biu.scapi.primitives.prf.cryptopp.CryptoPpAES;

/**
 * The {MinimizeAESSetKeyRowReductionGarbledBooleanCircuit} class is a utility class that computes the functionalities regarding Garbled Boolean Circuit 
 * that minimizes AES set key operations using the row reduction technique.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class MinimizeAESSetKeyRowReductionGarbledBooleanCircuitUtil extends StandardRowReductionGarbledBooleanCircuitUtil{
	
	private AES aes;
	
	/**
	 * Sets the given AES, KDF and random.
	 * @param AES to use in the computations
	 * @param kdf to use in the row reduction technique.
	 * @param random source of randomness.
	 * @param isRowReductionWithFixedOutputKeys indicates if the user is going to use sample the wires' keys out of given output keys. 
	 * In this case, the circuit representation should be a little different. 
	 * See {@link BooleanCircuit#BooleanCircuit(File f)} for more information.
	 */
	MinimizeAESSetKeyRowReductionGarbledBooleanCircuitUtil(AES aes, KeyDerivationFunction kdf, SecureRandom random, boolean isRowReductionWithFixedOutputKeys) {
		doConstruct(aes, kdf, random, isRowReductionWithFixedOutputKeys);
	}

	/**
	 * Sets the parameters.
	 */
	private void doConstruct(AES aes, KeyDerivationFunction kdf, SecureRandom random, boolean isRowReductionWithFixedOutputKeys) {
		this.aes = aes;
		this.random = random;
		this.kdf = kdf;
		 
		// This will be passed to the gates and used for decryption and (for now) verifying. Eventually, verifying will
	    // also minimize setKey operations and use aes directly.
	    mes = new AES128MultiKeyEncryption(aes);
	    this.isRowReductionWithFixedOutputKeys = isRowReductionWithFixedOutputKeys;
	}
	
	/**
	 * Default constructor. Uses CryptoPpAES, BcKdfISO18033 with SHA-1 and secureRandom objects.
	 * 
	 */
	MinimizeAESSetKeyRowReductionGarbledBooleanCircuitUtil() {
		try {
			doConstruct(new CryptoPpAES(), new BcKdfISO18033("SHA-1"), new SecureRandom(), false);
		} catch (FactoriesException e) {
			// Should not occur since SHA-1 is a valid digest name.
		}
	}
	
	/**
	 * Creates a MinimizeAESSetKeyRowReductionGate.
	 * @param ungarbledGate to garble.
	 * @param garbledTablesHolder
	 * @return the created gate.
	 */
	protected GarbledGate createGate(Gate ungarbledGate, GarbledTablesHolder garbledTablesHolder) {
		BitSet XORZeroTruthTable = new BitSet();
		XORZeroTruthTable.set(1);
		if(ungarbledGate.getTruthTable().equals(XORZeroTruthTable)){
			return new MinimizeAESSetKeyGarbledGate(ungarbledGate, mes, aes, garbledTablesHolder);
		} else{
			return new MinimizeAESSetKeyRowReductionGate(ungarbledGate, mes, aes, kdf, garbledTablesHolder);
		}
		
	}
	
	
}
