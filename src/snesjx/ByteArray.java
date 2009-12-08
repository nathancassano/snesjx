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

final class ByteArray 
{
	byte buffer[]; 
	
	ByteArray(int size)
	{
		buffer = new byte[size];
	}
	
	ByteArray( byte[] array )
	{
		buffer = array;
	}

	final byte getByte(int index)
	{
		return buffer[index];
	}
	
	final int get8Bit(int index)
	{
		return buffer[index] & 0xFF;
	}

	final int get16Bit(int index)
	{
		return ( buffer[index] & 0xFF ) | ( ( buffer[index+1] & 0xFF ) << 8 );
	}
	
	int get32Bit( int index )
	{
		return	( ( buffer[index] & 0xFF ) ) |
				( buffer[index + 1] & 0xFF ) << 8 |
				( buffer[index + 2] & 0xFF ) << 16 |
				( buffer[index + 3] & 0xFF ) << 24 ;
	}
	
	String getString( int index, int len ) {
		byte[] target = new byte[len];
		System.arraycopy(buffer, index, target, 0, len);
		return new String( target );
	}
	
	final String getString( ) {
		return new String( buffer );
	}
	
	byte[] getBytes( int index, int len ) {
		if( len + index > buffer.length ) {
			len = ( buffer.length - index );
		}
		byte[] target = new byte[len];
		System.arraycopy(buffer, index, target, 0, len);
		return target;
	}
	
	byte[] getBytes() {
		return buffer.clone();
	}
	
	ByteArray getRange( int index, int len ) {
		byte[] target = new byte[len];
		System.arraycopy(buffer, index, target, 0, len);
		return new ByteArray( target );
	}
	
	final String getHex( int index ) {
		return Integer.toHexString( get8Bit(index) );
	}
	
	final String getHexString( int index, int len ) {
		String temp = new String();
		if( len + index > buffer.length ) {
			len = ( buffer.length - index );
		}
		for (int i = 0; i < len; i++) {
			temp += Integer.toHexString( buffer[i + index] ) + " ";
		}
		return temp;
	}
	
	final void put8Bit(int index, int value)
	{
		buffer[index] = (byte) value;
	}
	
	final void put16Bit(int index, int value)
	{
		buffer[index + 1] = (byte) ( value >> 8 );
		buffer[index] = (byte) ( value & 0x00FF );
	}

	final void put32Bit(int index, int value)
	{
		buffer[index] = (byte) ( value & 0xFF );
		buffer[index + 1] = (byte) ( ( value >> 8 ) & 0x00FF ); 
		buffer[index + 2] = (byte) ( ( value >> 16 ) & 0x00FF );
		buffer[index + 3] = (byte) ( value >>> 24 );
	}
		
	boolean compare(int index, byte[] array )
	{
		for (int i = 0; i < array.length; i++) {
			if (buffer[i + index] != array[i]) return false;
		}
		
		return true;
	}
	
	final void arraycopy(int destPos, ByteArray src, int srcPos, int length)
	{
		System.arraycopy(src.buffer, srcPos, buffer, destPos, length);
	}
	
	final void arraycopy(int destPos, ByteArrayOffset src, int srcPos, int length)
	{
		System.arraycopy(src.buffer, src.getOffset() + srcPos, buffer, destPos, length);
	}
	
	void zero()
	{
		for(int i = 0; i < buffer.length; i++)
			buffer[i] = 0;
	}
	
	final int size()
	{
		return buffer.length;
	}
	
	void fill(int value, int index, int length)
	{
		for (length += index; index < length; index++)
			put8Bit(index, value);
	}

	ByteArrayOffset getOffsetBuffer(int offset)
	{
		return new ByteArrayOffset(this, offset);
	}
}


