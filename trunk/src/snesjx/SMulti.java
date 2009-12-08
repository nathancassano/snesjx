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

class SMulti
{
	int cartType;
	int cartSizeA, cartSizeB;
	int sramSizeA, sramSizeB;
	int sramMaskA, sramMaskB;
	int cartOffsetA, cartOffsetB;
	ByteArray sramA;
	ByteArray sramB;
	String	fileNameA, fileNameB;
	
	void zero()
	{
		cartType = 0;
		cartSizeA = 0;
		cartSizeB = 0;
		sramSizeA = 0;
		sramSizeB = 0;
		sramMaskA = 0;
		sramMaskB = 0;
		cartOffsetA = 0;
		cartOffsetB = 0;
		sramA = null;
		sramB = null;
		fileNameA = null;
		fileNameB = null;
	}
}
