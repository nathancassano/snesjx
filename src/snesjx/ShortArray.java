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

class ShortArray
{
	short buffer[];
	int offset = 0;
	
	ShortArray(int size)
	{
		buffer = new short[size];
	}
	
	ShortArray( short[] array )
	{
		buffer = array;
	}

	final short get16Bit(int index)
	{
		return buffer[index + offset];
	}
		
	final void put16Bit(int index, int value)
	{
		buffer[index + offset] = (short) ( value );
	}

	short[] array()
	{
		return buffer;
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
	
	final int getOffset()
	{
		return this.offset;
	}
	
	final void setOffset( int value )
	{
		this.offset = value;
	}
	
	ShortArray getOffsetBuffer(int offset)
	{
		ShortArray shortbuffer = new ShortArray(this.buffer);
		
		shortbuffer.setOffset(offset);
		
		return shortbuffer;
	}
}


