package com.jiawen.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jiawen.Hex2Bin;

public class TestHex2Bin {

	@Test
	public void testGetWord() {
		assertEquals(1, Hex2Bin.getWord("0001", 0));
		assertEquals(16, Hex2Bin.getWord("0010", 0));
		assertEquals(256, Hex2Bin.getWord("0100", 0));
		assertEquals(4096, Hex2Bin.getWord("1000", 0));
		assertEquals(4369, Hex2Bin.getWord("01111", 1));
	}

}
