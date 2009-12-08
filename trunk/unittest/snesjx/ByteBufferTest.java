/*
 * SnesJx - Portable Super Nintendo Entertainment System (TM) emulator.
 * 
 * (C) Copyright 2009 Nathan Cassano
 *
 * Permission to use, copy, modify and distribute SnesJx in both binary and
 * source form, for non-commercial purposes, is hereby granted without fee,
 * providing that this license information and copyright notice appear with
 * all copies and any derived work.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event shall the authors be held liable for any damages
 * arising from the use of this software.
 *
 * SnesJx is freeware for PERSONAL USE only. Commercial users should
 * seek permission of the copyright holders first. Commercial use includes
 * charging money for SnesJx or software derived from SnesJx.
 *
 * The copyright holders request that bug fixes and improvements to the code
 * should be forwarded to them so everyone can benefit from the modifications
 * in future versions.
 *
 * Super NES and Super Nintendo Entertainment System are trademarks of
 * Nintendo Co., Limited and its subsidiary companies.
 */

package snesjx;

import junit.framework.TestCase;

public class ByteBufferTest extends TestCase {

	private ByteArray buf;
	
	protected void setUp() throws Exception {
		super.setUp();
		buf = new ByteArray( "AAAAABBBBBCCCCCDDDDD".getBytes() );
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGet8Bit() {
		
		assertEquals( 0x41, buf.get8Bit( 0 ) );
		assertEquals( 0x44, buf.get8Bit( 15 ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( 0x43, offset.get8Bit( 0 ) );
	}

	public void testGet16Bit() {
		assertEquals( 0x4141, buf.get16Bit(0) );
		assertEquals( 0x4443, buf.get16Bit(14) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( 0x4343, offset.get16Bit(0) );
		assertEquals( 0x4443, offset.get16Bit(4) );
	}

	public void testGet32Bit() {
		
		assertEquals( 0x41414141, buf.get32Bit(0) );
		assertEquals( 0x42424141, buf.get32Bit(3) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( 0x43434343, offset.get32Bit(0) );
		assertEquals( 0x44444343, offset.get32Bit(3) );
		
	}
	
	public void testPut8Bit() {
		buf.put8Bit(10, 0x45);
		assertEquals( "AAAAABBBBBECCCCDDDDD", new String( buf.buffer ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		offset.put8Bit(5, 0x45);
		assertEquals( "AAAAABBBBBECCCCEDDDD", new String( buf.buffer ) );
		assertEquals( 0x45, offset.get8Bit(5) );
	}


	public void testGetBytes() {
		assertEquals( "AAAAA", new String( buf.getBytes(0, 5) ) );
		assertEquals( "CCCCC", new String( buf.getBytes(10, 5) ) );
		assertEquals( "DDDDD", new String( buf.getBytes(15, 10) ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( "CCCCC", new String( offset.getBytes(0, 5) ) );
		assertEquals( "DDDDD", new String( offset.getBytes(5, 5) ) );
		assertEquals( "DDDDD", new String( offset.getBytes(5, 10) ) );
	}
	
	public void testPut16Bit() {
		
		buf.put16Bit(4, 0x4645);
		assertEquals( "AAAAEFBBBBCCCCCDDDDD", new String( buf.buffer ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		offset.put16Bit(4, 0x4645);
		assertEquals( "AAAAEFBBBBCCCCEFDDDD", new String( buf.buffer ) );
	}
	
	public void testPut32Bit() {
		
		buf.put32Bit(0, 0x48474645);
		assertEquals( "EFGHABBBBBCCCCCDDDDD", new String( buf.buffer ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		offset.put32Bit(0, 0x48474645);
		assertEquals( "EFGHABBBBBEFGHCDDDDD", new String( buf.buffer ) );
		
	}
	
	public void testCompare() {
		assertEquals( true , buf.compare(0, "AAAAA".getBytes() ) );

		assertEquals( true , buf.compare(10, "CCCCC".getBytes() ) );
		
		assertEquals( false , buf.compare(15, "ZZZZ".getBytes() ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( true , offset.compare(0, "CCCCC".getBytes() ) );
		assertEquals( true , offset.compare(5, "DDDDD".getBytes() ) );
	}


	public void testArraycopy() {
		ByteArray temp = new ByteArray( "GGGGG".getBytes() );
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		
		buf.arraycopy(5, temp, 0, 5);
		assertEquals( "AAAAAGGGGGCCCCCDDDDD", new String( buf.buffer ) );
		
		offset.arraycopy(5, temp, 0, 5);
		assertEquals( "AAAAAGGGGGCCCCCGGGGG", new String( offset.buffer ) );
		
	}

	public void testZero() {
		buf.zero();
		//assertEquals( "00000000000000000000", new String( buf.buffer ) );
	}

	public void testSize() {

		assertEquals( 20, buf.size() );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( 10, offset.size() );
	}

	public void testFill() {
		
		buf.fill(0x32, 10, 10);
		assertEquals( "AAAAABBBBB2222222222", new String( buf.buffer ) );
		buf.fill(0x32, 0, 10);
		assertEquals( "22222222222222222222", new String( buf.buffer ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		offset.fill(0x31, 0, 10);
		assertEquals( "22222222221111111111", new String( buf.buffer ) );
	}

	public void testToString() {
		assertEquals( "AAAAABBBBB", buf.getString(0, 10) );
		assertEquals( "CCCCC", buf.getString( 10, 5) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( "CCCCC", offset.getString( 0, 5) );
		assertEquals( "DDDDD", offset.getString( 5, 5) );
	}

	public void testGetHex() {	
		assertEquals( "41", buf.getHex(0) );
		assertEquals( "43", buf.getHex(12) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( "43", offset.getHex(0) );
		assertEquals( "44", offset.getHex(5) );
	}
	
	public void testGetHexString() {	
		assertEquals( "41 41 41 41 41 ", buf.getHexString(0, 5) );
		assertEquals( "44 44 44 44 44 ", buf.getHexString(15, 5) );
		assertEquals( "44 44 44 44 44 ", buf.getHexString(15, 10) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( "43 43 43 43 43 ", offset.getHexString(0, 5) );
		assertEquals( "44 44 44 44 44 ", offset.getHexString(5, 5) );
		assertEquals( "44 44 44 44 44 ", offset.getHexString(5, 10) );
		
	}
	
	
	public void testGetRange() {	
		buf.getRange(0, 10);
		assertEquals( "AAAAABBBBB", new String( buf.getRange(0, 10).buffer ) );
		
		ByteArrayOffset offset = buf.getOffsetBuffer( 10 );
		assertEquals( "DDDDD", new String( offset.getRange(5, 5).buffer ) );

	}
	
	public void testByteBufferOffset() {	
		ByteArrayOffset offset1 = buf.getOffsetBuffer( 5 );
		ByteArrayOffset offset = offset1.getOffsetBuffer( 5 );
		assertEquals( "CCCCC", offset.getString( 0, 5) );
		assertEquals( "DDDDD", offset.getString( 5, 5) );
	}
	public void testSign() {
		buf.put8Bit(0, 0xFF );
		assertEquals( 255, buf.get8Bit(0) );
		
		int sum = 0;
		sum += buf.get8Bit(0);
		
	}
}
