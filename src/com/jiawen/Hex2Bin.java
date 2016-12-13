package com.jiawen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

/**
 * A little utility to convert Intel Hex file to BIN file. The format of Intel
 * Hex can be found at https://en.wikipedia.org/wiki/Intel_HEX.
 * 
 *
 * @author xiong.jia.wen@163.com
 * @date 2016-11-13
 * @version 1.0
 */
public class Hex2Bin {
	// Record type: Data
	private final static int REC_TYPE_DATA = 0;
	// Record type: End Of File
	private final static int REC_TYPE_END_OF_FILE = 1;
	// Record type: Extended Segment Address
	private final static int REC_TYPE_EXTEND_SEG_ADDR = 2;
	// Record type: Start Segment Address
	private final static int REC_TYPE_START_SEG_ADDR = 3;
	// Record type: Extended Linear Address
	private final static int REC_TYPE_EXTEND_LINEAR_ADDR = 4;
	// Record type: Start Linear Address
	private final static int REC_TYPE_START_LINEAR_ADDR = 5;

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.err.printf("usage: hex2bin in.hex out.bin [-offset]\n");
			System.exit(-1);
		}
		File inFile = new File(args[0]);
		File outFile = new File(args[1]);
		BufferedReader reader = null;
		RandomAccessFile raf = null;

		String line = null;
		int numofBytes = 0;
		int numofLine = 0;
		byte checksum = 0;
		int indexInLine = 0;
		int segAddr = 0;
		int baseAddr = 0;
		int addr = 0;
		int recType = 0;
		int b;
		int startExecution = 0; // start address;
		boolean ifoffset = false;
		if (args[2].equals("-offset")) {
			ifoffset = true;
		}

		// hashset
		Set set = new HashSet<Integer>();
		try {
			reader = new BufferedReader(new FileReader(inFile));
			raf = new RandomAccessFile(outFile, "rw");
			while ((line = reader.readLine()) != null) {
				numofLine++;
				// Start code: first character must be ':'
				if (line.charAt(0) != ':') {
					System.err.printf("Bad format on line %d: %s\n", numofLine, line);
					System.exit(-1);
				}

				indexInLine = 0;
				checksum = 0;
				indexInLine++;

				// Byte count: next two characters indicate the number of bytes
				// in data field
				numofBytes = getByte(line, indexInLine);
				if (numofBytes == 0) {
					break;
				}
				checksum += numofBytes;
				indexInLine += 2;

				// Address: next four characters indicate the address
				addr = getWord(line, indexInLine);
				checksum += getByte(line, indexInLine);
				checksum += getByte(line, indexInLine + 2);
				indexInLine += 4;

				// Record type: next two characters indicate type of the record
				recType = getByte(line, indexInLine);
				checksum += recType;
				indexInLine += 2;
				set.add(recType);
				switch (recType) {
				case REC_TYPE_DATA:
					addr = (segAddr << 4) + baseAddr + addr;
					for (int i = 0; i < numofBytes; i++) {
						b = getByte(line, indexInLine);
						checksum += b;
						indexInLine += 2;
						raf.seek(addr);
						raf.writeByte(b);
						addr++;
					}
					break;
				case REC_TYPE_END_OF_FILE:
					break;
				case REC_TYPE_EXTEND_SEG_ADDR:
					segAddr = getWord(line, indexInLine);
					checksum += getByte(line, indexInLine);
					checksum += getByte(line, indexInLine + 2);
					indexInLine += 4;
					break;
				case REC_TYPE_START_SEG_ADDR:
					/*
					 * For 80x86 processors, specifies the initial content of
					 * the CS:IP registers. The address field is 0000, the byte
					 * count is 04, the first two bytes are the CS value, the
					 * latter two are the IP value.
					 */
					checksum += getByte(line, indexInLine);
					checksum += getByte(line, indexInLine + 2);
					indexInLine += 4;
					break;
				case REC_TYPE_EXTEND_LINEAR_ADDR:
					// 04
					if (ifoffset) {
						baseAddr = (getWord(line, indexInLine) << 16);
					}
					checksum += getByte(line, indexInLine);
					checksum += getByte(line, indexInLine + 2);
					indexInLine += 4;
					System.out.printf("Line %d:base address change to 0x%08x\n", numofLine, baseAddr);
					break;
				case REC_TYPE_START_LINEAR_ADDR:
					// 05
					/*
					 * The four data bytes represent the 32-bit value loaded
					 * into the EIP register of the 80386 and higher CPU.
					 */
					startExecution = (getWord(line, indexInLine) << 16) + getWord(line, indexInLine + 4);
					checksum += getByte(line, indexInLine);
					checksum += getByte(line, indexInLine + 2);
					checksum += getByte(line, indexInLine + 4);
					checksum += getByte(line, indexInLine + 6);
					indexInLine += 8;
					break;
				default:
					System.out.printf("Record type isn't in 0-4\n");
					break;
				}

				checksum = (byte) (~checksum + 1);
				// Checksum : the last character is checksum
				byte actualChecksum = (byte) getByte(line, indexInLine);
				if (checksum != actualChecksum) {
					System.out.printf("Checksum mismatch on line %d: %02x vs %02x\n", numofLine, checksum,
							actualChecksum);
				}
			}
			if (startExecution >= 0)
				System.out.printf("start execution at 0x%08X\n", startExecution);
			System.out.printf("The following Record Type has emerged: %s.\n", set.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e2) {
				// TODO: handle exception
			}

		}

	}

	public static int getnybble(String str, int index) {
		int value = 0;
		char c = str.charAt(index);
		if (c >= '0' && c <= '9') {
			value = c - '0';
		} else if (c >= 'a' && c <= 'f') {
			value = c - 'a' + 10;
		} else if (c >= 'A' && c <= 'F') {
			value = c - 'A' + 10;
		}
		return value;
	}

	public static int getByte(String str, int index) {
		return (getnybble(str, index) << 4) + getnybble(str, index + 1);
	}

	public static int getWord(String str, int index) {
		return (getByte(str, index) << 8) + getByte(str, index + 2);
	}
}
