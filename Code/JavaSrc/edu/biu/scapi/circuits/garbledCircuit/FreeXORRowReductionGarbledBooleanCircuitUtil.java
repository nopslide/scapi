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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import edu.biu.scapi.circuits.circuit.BooleanCircuit;
import edu.biu.scapi.circuits.circuit.Gate;
import edu.biu.scapi.circuits.encryption.MultiKeyEncryptionScheme;
import edu.biu.scapi.exceptions.NoSuchPartyException;
import edu.biu.scapi.exceptions.PlaintextTooLongException;
import edu.biu.scapi.primitives.kdf.KeyDerivationFunction;

/**
 * The {FreeXORRowReductionGarbledBooleanCircuit} class is a utility class that computes the functionalities regarding Free XOR Garbled Boolean Circuit 
 * using the row reduction technique.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
class FreeXORRowReductionGarbledBooleanCircuitUtil extends FreeXORGarbledBooleanCircuitUtil {

	private KeyDerivationFunction kdf;
	private boolean isRowReductionWithFixedOutputKeys;
	
	/**
	 * Sets the given MultiKeyEncryptionScheme and kdf.
	 * @param mes
	 * @param kdf
	 * @param isRowReductionWithFixedOutputKeys indicates if the user is going to use sample the wires' keys out of given output keys. 
	 * In this case, the circuit representation should be a little different. 
	 * See {@link BooleanCircuit#BooleanCircuit(File f)} for more information.
	 */
	FreeXORRowReductionGarbledBooleanCircuitUtil(MultiKeyEncryptionScheme mes, KeyDerivationFunction kdf, boolean isRowReductionWithFixedOutputKeys) {
		super(mes);
		this.kdf = kdf;
		this.isRowReductionWithFixedOutputKeys = isRowReductionWithFixedOutputKeys;
	}
	
	/**
	 * Default constructor.
	 */
	FreeXORRowReductionGarbledBooleanCircuitUtil(){
		super();
	}
	
	@Override
	protected GarbledGate createStandardGate(Gate ungarbledGate, GarbledTablesHolder garbledTablesHolder) {
		BitSet XORZeroTruthTable = new BitSet();
		XORZeroTruthTable.set(1);
		//In case the truth table is 01 this is the last gate that was added in order to allow sampling keys out of given output keys.
		//The last gate should not use the row reduction technique.
		if(ungarbledGate.getTruthTable().equals(XORZeroTruthTable)){
			return new StandardGarbledGate(ungarbledGate, mes, garbledTablesHolder);
		} else{
			return new StandardRowReductionGarbledGate(ungarbledGate, mes, kdf, garbledTablesHolder);
		}
	}
	
	/**
	 * Generates wires' keys.<P>
	 * In case of row reduction, the user can give the output keys only if he set the isRowReductionWithFixedOutputKeys to true in the constructor.
	 * @throws IllegalArgumentException if the user gave the output keys while the isRowReductionWithFixedOutputKeys set to false.
	 */
	@Override
	public CircuitCreationValues generateWireKeysAndSetTables(BooleanCircuit ungarbledCircuit, GarbledTablesHolder garbledTablesHolder, 
			GarbledGate[] gates, Map<Integer, SecretKey[]> partialWireValues, HashMap<Integer, Byte> signalBits) {
		if (partialWireValues.containsKey(ungarbledCircuit.getOutputWireLabels()[0]) && !isRowReductionWithFixedOutputKeys){
			throw new IllegalArgumentException("Cannot accept output wires' keys when Row Reduction with fixed keys is not declared");
		}
		//Prepare the maps that will be used during keys generation.
		Map<Integer, SecretKey[]> allWireValues = new HashMap<Integer, SecretKey[]>();
		Map<Integer, SecretKey[]> allInputWireValues = null;
		Map<Integer, SecretKey[]> allOutputWireValues = null;
		Map<Integer, Byte> allSignalBits = new HashMap<Integer, Byte>();
		HashMap<Integer, Byte> translationTable = new HashMap<Integer, Byte>();
		HashMap<Integer, Byte> inputSignalBits = new HashMap<Integer, Byte>();
		Gate[] ungarbledGates = ungarbledCircuit.getGates();
		byte[] globalKeyOffset = null;
		
		//If there are partial keys, check if they are the input keys or the output keys and set them.
		if (!partialWireValues.isEmpty()){
			if (partialWireValues.containsKey(ungarbledCircuit.getOutputWireLabels()[0])){
				allOutputWireValues = partialWireValues;
				translationTable = signalBits;
				//Deduce the global key offset from allOutputWireValues.
				globalKeyOffset = extractGlobalkey(allOutputWireValues, ungarbledCircuit.getOutputWireLabels()[0]);
			} else{
				allInputWireValues = partialWireValues;
				inputSignalBits = signalBits;
				try {
					globalKeyOffset = extractGlobalkey(allInputWireValues, ungarbledCircuit.getInputWireLabels(1).get(0));
				} catch (NoSuchPartyException e) {
					//1 should be a valid party number
				}
			}
			allWireValues.putAll(partialWireValues);
			allSignalBits.putAll(signalBits);
		} else {
			/*
			 * The globalKeyOffset is a randomly chosen bit sequence that is the same size as the key and will be used to create the 
			 * garbled wire's values.
			 * We used generate key since this way globalKeyOfset will always be the size of the key. 
			 * See Free XOR Gates and Applications by Validimir Kolesnikov and Thomas Schneider.
			 */
			globalKeyOffset = mes.generateKey().getEncoded();
			/*
			 * Setting the last bit to 1. This follows algorithm 1 step 2 part A of Free XOR Gates and Applications by Validimir 
			 * Kolesnikov and Thomas Schneider.
			 * This algorithm calls for XORing the Wire values with R and the signal bit with 1. So, we set the last bit of R to 1 and 
			 * this will be XOR'd with the last bit of the wire value, which is the signal bit in our implementation.
			 */
			globalKeyOffset[globalKeyOffset.length - 1] |= 1;
		}
		
		//In case the keys for the input wires have not been sampled yet, sample them.
		if (allInputWireValues == null){
			//Sample input keys.
			allInputWireValues = new HashMap<Integer, SecretKey[]>();
			for (int i=1; i<=ungarbledCircuit.getNumberOfParties(); i++){
				ArrayList<Integer> inputWireLabels = null;
				try {
					inputWireLabels = ungarbledCircuit.getInputWireLabels(i);
				} catch (NoSuchPartyException e) {
					// should not occur since the number is a valid party number
				}
				for (int w : inputWireLabels) {
					//Samples random key. The other key will be calculated via XOR with the globalKeyOffset.
					SecretKey zeroValue = mes.generateKey();
					sampleInputKeys(allInputWireValues, inputSignalBits, globalKeyOffset, w, zeroValue);
				}
			}
			allSignalBits.putAll(inputSignalBits);
			allWireValues.putAll(allInputWireValues);
		}
		
		
		allOutputWireValues = new HashMap<Integer, SecretKey[]>();
		translationTable = new HashMap<Integer, Byte>();
		
		//Create the keys of the non-input wires.
		createNonInputWireValues(ungarbledGates, allWireValues, allSignalBits, globalKeyOffset);
		
		//Fill the the output wire values to be used in the following sub circuit
		for (int n : ungarbledCircuit.getOutputWireLabels()) {
			
			//Add both values of output wire labels to the allOutputWireLabels Map that was passed as a parameter.
			allOutputWireValues.put(n, allWireValues.get(n));
			translationTable.put(n, allSignalBits.get(n));
		}
		
		
		//now that we have all keys, we can create the garbled tables.
		try {
			createGarbledTables(gates, garbledTablesHolder, ungarbledGates, allWireValues, allSignalBits);
		} catch (InvalidKeyException e) {
			// Should not occur since the keys were generated through the encryption scheme that generates keys that match it.
		} catch (IllegalBlockSizeException e) {
			// Should not occur since the keys were generated through the encryption scheme that generates keys that match it.
		} catch (PlaintextTooLongException e) {
			// Should not occur since the keys were generated through the encryption scheme that generates keys that match it.
		} 
		
		return new CircuitCreationValues(allInputWireValues, inputSignalBits, allOutputWireValues, translationTable);	
	}
	
	/**
	 * Generates keys for standard gate in the row reduction technique.
	 * @param zeroValueBytes this value is ignored since the row reduction technique calculates both values from the gate's input keys.
	 */
	protected void generateStandardValues(Gate ungarbledGate, Map<Integer, SecretKey[]> allWireValues,
			Map<Integer, Byte> signalBits, byte[] globalKeyOffset, byte[] zeroValueBytes) {
		BitSet XORZeroTruthTable = new BitSet();
		XORZeroTruthTable.set(1);
		if(!ungarbledGate.getTruthTable().equals(XORZeroTruthTable)){
			int[] labels = ungarbledGate.getInputWireLabels();
			int numberOfInputs = labels.length;
			//number of rows is 2^numberOfInputs - 1. The last row will be calculated by the row reduction technique.
			int numberOfRows = (int) Math.pow(2, numberOfInputs)-1;
			
			//Find the line that we do not save in the table and we use the KDF to find the value of the output key.
			for (int rowOfTruthTable = 0; (rowOfTruthTable <= numberOfRows) && !(allWireValues.containsKey(ungarbledGate.getOutputWireLabels()[0])); rowOfTruthTable++) {
			  	int permutedPosition = 0;
			  	for (int i = 0, j = (int) Math.pow(2, numberOfInputs - 1), reverseIndex = numberOfInputs - 1; i < numberOfInputs; i++, j /= 2, reverseIndex--) {
			  	
			  		/* Truth table inputs are arranged according to binary number values. j is the value that begins as a 1 in the
				   	 * leftmost(most significant bit) of the binary number that is the size of the truth table. Say for example that there are
				   	 * 3 inputs. So the truth table has 3 input columns. j begins as the binary number 100 and we use it to check whether the leftmost bit in
				   	 * the row of the truth table is set. If it is, that means that the input value is a 1.  Otherwise it's a 0. We then divide j by 2 to obtain the binary
				   	 * number 010 and we use this to determine the value of the inputs in the second column. We then divide by 2 again to obtain the 
				   	 * binary number 001 and use it to determine the value of the inputs in the third column.
				   	 */
			  		int input = (((rowOfTruthTable & j) == 0) ? 0 : 1);
			  		byte signalBit = signalBits.get(labels[i]);
			  		permutedPosition += (input ^ signalBit) * (Math.pow(2, reverseIndex));		
			  	
			  	}
			  	
			  	//This is the row that we do not save in the table but calculate the value via KDF.
			  	if (permutedPosition == numberOfRows){
			  		//Get the indexes of the input keys.
			  		int wire0Key = ((rowOfTruthTable & 2) == 0) ? 0 : 1;
			  		int wire1Key = ((rowOfTruthTable & 1) == 0) ? 0 : 1;
			  		
			  		//Get the input keys.
			  		SecretKey input0 = allWireValues.get(labels[0])[wire0Key];
			  		SecretKey input1 = allWireValues.get(labels[1])[wire1Key];
			  		
			  		//Calculate the output key via KDF.
			  		ByteBuffer kdfBytes = ByteBuffer.allocate(mes.getCipherSize()*numberOfInputs +16);
					kdfBytes.put(input0.getEncoded());
					kdfBytes.put(input1.getEncoded());
					kdfBytes.putInt(ungarbledGate.getGateNumber());
					kdfBytes.putInt((input0.getEncoded()[input0.getEncoded().length - 1] & 1) == 0 ? 0 : 1);
					kdfBytes.putInt((input1.getEncoded()[input1.getEncoded().length - 1] & 1) == 0 ? 0 : 1);
					SecretKey wireValue = kdf.deriveKey(kdfBytes.array(), 0, mes.getCipherSize()*numberOfInputs +16, mes.getCipherSize());
					byte[] kdfResultBytes = wireValue.getEncoded();
					
					//Calculate the other value by xoring the kdf result with the globalKeyOffset
					byte[] otherBytes = new byte[mes.getCipherSize()];
					for (int i = 0; i < zeroValueBytes.length; i++) {
						otherBytes[i] = (byte) (kdfResultBytes[i] ^ globalKeyOffset[i]);
					}
					
					//Now we have both values, calculate the signal bit.
					boolean output = ungarbledGate.getTruthTable().get(rowOfTruthTable);
					byte signalBit = 0;
					SecretKey zeroKey, oneKey;
					if (output == false){
						zeroKey = wireValue;
						signalBit = (byte) (wireValue.getEncoded()[wireValue.getEncoded().length - 1] & 1);
						oneKey = new SecretKeySpec(otherBytes, "");
					} else {
						oneKey = wireValue;
						signalBit = (byte) (1 - (wireValue.getEncoded()[wireValue.getEncoded().length - 1] & 1));
						zeroKey = new SecretKeySpec(otherBytes, "");
					}
					
					//Put k0, k1 and signal bit in the map.
					signalBits.put(ungarbledGate.getOutputWireLabels()[0], signalBit);
					allWireValues.put(ungarbledGate.getOutputWireLabels()[0], new SecretKey[] {zeroKey, oneKey});
						
			  	}
			}
		}
	}
	
	
}
