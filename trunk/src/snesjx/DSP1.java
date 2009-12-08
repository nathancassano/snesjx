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

class DSP1
{
	static final int M_DSP1_LOROM_S = 0;
	static final int M_DSP1_LOROM_L = 1;
	static final int M_DSP1_HIROM = 2;
	static final int M_DSP2_LOROM = 3;
	static final int M_DSP3_LOROM = 4;
	static final int M_DSP4_LOROM = 5;

	short version;
	private boolean waiting4command;
	private boolean first_parameter;

	private int command;
	private int in_count;
	private int in_index;
	private int out_count;
	private int out_index;
	private ByteArray parameters = new ByteArray(512);
	private ByteArray output = new ByteArray(512);
	
	int maptype;
	int boundary;
	
	// TODO: Is this referenced correctly?
	private DSP4 dsp4 = new DSP4(); 
	
	private static boolean init = false;
	
	private static int A, B, C, D;

	private static int CentreX;
	private static int CentreY;
	private static int VOffset;

	private static int VPlane_C;
	private static int VPlane_E;

	// Azimuth and Zenith angles
	private static int SinAas;
	private static int CosAas;
	private static int SinAzs;
	private static int CosAzs;

	// Clipped Zenith angle
	private static int SinAZS;
	private static int CosAZS;
	private static int SecAZS_C1;
	private static int SecAZS_E1;
	private static int SecAZS_C2;
	private static int SecAZS_E2;

	private static int Nx, Ny, Nz;
	private static int Gx, Gy, Gz;
	private static int C_Les, E_Les, G_Les;

	private static int[][] matrixC = new int[3][3];
	private static int[][] matrixB = new int[3][3];
	private static int[][] matrixA = new int[3][3];

	
	private static final int DSP1ROM[] = {
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0001,	0x0002,	0x0004,	0x0008,	0x0010,	0x0020,
		0x0040,	0x0080,	0x0100,	0x0200,	0x0400,	0x0800,	0x1000,	0x2000,
		0x4000,	0x7fff,	0x4000,	0x2000,	0x1000,	0x0800,	0x0400,	0x0200,
		0x0100,	0x0080,	0x0040,	0x0020,	0x0001,	0x0008,	0x0004,	0x0002,
		0x0001,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,	0x0000,
		0x0000,	0x0000,	0x8000,	0xffe5,	0x0100,	0x7fff,	0x7f02,	0x7e08,
		0x7d12,	0x7c1f,	0x7b30,	0x7a45,	0x795d,	0x7878,	0x7797,	0x76ba,
		0x75df,	0x7507,	0x7433,	0x7361,	0x7293,	0x71c7,	0x70fe,	0x7038,
		0x6f75,	0x6eb4,	0x6df6,	0x6d3a,	0x6c81,	0x6bca,	0x6b16,	0x6a64,
		0x69b4,	0x6907,	0x685b,	0x67b2,	0x670b,	0x6666,	0x65c4,	0x6523,
		0x6484,	0x63e7,	0x634c,	0x62b3,	0x621c,	0x6186,	0x60f2,	0x6060,
		0x5fd0,	0x5f41,	0x5eb5,	0x5e29,	0x5d9f,	0x5d17,	0x5c91,	0x5c0c,
		0x5b88,	0x5b06,	0x5a85,	0x5a06,	0x5988,	0x590b,	0x5890,	0x5816,
		0x579d,	0x5726,	0x56b0,	0x563b,	0x55c8,	0x5555,	0x54e4,	0x5474,
		0x5405,	0x5398,	0x532b,	0x52bf,	0x5255,	0x51ec,	0x5183,	0x511c,
		0x50b6,	0x5050,	0x4fec,	0x4f89,	0x4f26,	0x4ec5,	0x4e64,	0x4e05,
		0x4da6,	0x4d48,	0x4cec,	0x4c90,	0x4c34,	0x4bda,	0x4b81,	0x4b28,
		0x4ad0,	0x4a79,	0x4a23,	0x49cd,	0x4979,	0x4925,	0x48d1,	0x487f,
		0x482d,	0x47dc,	0x478c,	0x473c,	0x46ed,	0x469f,	0x4651,	0x4604,
		0x45b8,	0x456c,	0x4521,	0x44d7,	0x448d,	0x4444,	0x43fc,	0x43b4,
		0x436d,	0x4326,	0x42e0,	0x429a,	0x4255,	0x4211,	0x41cd,	0x4189,
		0x4146,	0x4104,	0x40c2,	0x4081,	0x4040,	0x3fff,	0x41f7,	0x43e1,
		0x45bd,	0x478d,	0x4951,	0x4b0b,	0x4cbb,	0x4e61,	0x4fff,	0x5194,
		0x5322,	0x54a9,	0x5628,	0x57a2,	0x5914,	0x5a81,	0x5be9,	0x5d4a,
		0x5ea7,	0x5fff,	0x6152,	0x62a0,	0x63ea,	0x6530,	0x6672,	0x67b0,
		0x68ea,	0x6a20,	0x6b53,	0x6c83,	0x6daf,	0x6ed9,	0x6fff,	0x7122,
		0x7242,	0x735f,	0x747a,	0x7592,	0x76a7,	0x77ba,	0x78cb,	0x79d9,
		0x7ae5,	0x7bee,	0x7cf5,	0x7dfa,	0x7efe,	0x7fff,	0x0000,	0x0324,
		0x0647,	0x096a,	0x0c8b,	0x0fab,	0x12c8,	0x15e2,	0x18f8,	0x1c0b,
		0x1f19,	0x2223,	0x2528,	0x2826,	0x2b1f,	0x2e11,	0x30fb,	0x33de,
		0x36ba,	0x398c,	0x3c56,	0x3f17,	0x41ce,	0x447a,	0x471c,	0x49b4,
		0x4c3f,	0x4ebf,	0x5133,	0x539b,	0x55f5,	0x5842,	0x5a82,	0x5cb4,
		0x5ed7,	0x60ec,	0x62f2,	0x64e8,	0x66cf,	0x68a6,	0x6a6d,	0x6c24,
		0x6dca,	0x6f5f,	0x70e2,	0x7255,	0x73b5,	0x7504,	0x7641,	0x776c,
		0x7884,	0x798a,	0x7a7d,	0x7b5d,	0x7c29,	0x7ce3,	0x7d8a,	0x7e1d,
		0x7e9d,	0x7f09,	0x7f62,	0x7fa7,	0x7fd8,	0x7ff6,	0x7fff,	0x7ff6,
		0x7fd8,	0x7fa7,	0x7f62,	0x7f09,	0x7e9d,	0x7e1d,	0x7d8a,	0x7ce3,
		0x7c29,	0x7b5d,	0x7a7d,	0x798a,	0x7884,	0x776c,	0x7641,	0x7504,
		0x73b5,	0x7255,	0x70e2,	0x6f5f,	0x6dca,	0x6c24,	0x6a6d,	0x68a6,
		0x66cf,	0x64e8,	0x62f2,	0x60ec,	0x5ed7,	0x5cb4,	0x5a82,	0x5842,
		0x55f5,	0x539b,	0x5133,	0x4ebf,	0x4c3f,	0x49b4,	0x471c,	0x447a,
		0x41ce,	0x3f17,	0x3c56,	0x398c,	0x36ba,	0x33de,	0x30fb,	0x2e11,
		0x2b1f,	0x2826,	0x2528,	0x2223,	0x1f19,	0x1c0b,	0x18f8,	0x15e2,
		0x12c8,	0x0fab,	0x0c8b,	0x096a,	0x0647,	0x0324,	0x7fff,	0x7ff6,
		0x7fd8,	0x7fa7,	0x7f62,	0x7f09,	0x7e9d,	0x7e1d,	0x7d8a,	0x7ce3,
		0x7c29,	0x7b5d,	0x7a7d,	0x798a,	0x7884,	0x776c,	0x7641,	0x7504,
		0x73b5,	0x7255,	0x70e2,	0x6f5f,	0x6dca,	0x6c24,	0x6a6d,	0x68a6,
		0x66cf,	0x64e8,	0x62f2,	0x60ec,	0x5ed7,	0x5cb4,	0x5a82,	0x5842,
		0x55f5,	0x539b,	0x5133,	0x4ebf,	0x4c3f,	0x49b4,	0x471c,	0x447a,
		0x41ce,	0x3f17,	0x3c56,	0x398c,	0x36ba,	0x33de,	0x30fb,	0x2e11,
		0x2b1f,	0x2826,	0x2528,	0x2223,	0x1f19,	0x1c0b,	0x18f8,	0x15e2,
		0x12c8,	0x0fab,	0x0c8b,	0x096a,	0x0647,	0x0324,	0x0000,	0xfcdc,
		0xf9b9,	0xf696,	0xf375,	0xf055,	0xed38,	0xea1e,	0xe708,	0xe3f5,
		0xe0e7,	0xdddd,	0xdad8,	0xd7da,	0xd4e1,	0xd1ef,	0xcf05,	0xcc22,
		0xc946,	0xc674,	0xc3aa,	0xc0e9,	0xbe32,	0xbb86,	0xb8e4,	0xb64c,
		0xb3c1,	0xb141,	0xaecd,	0xac65,	0xaa0b,	0xa7be,	0xa57e,	0xa34c,
		0xa129,	0x9f14,	0x9d0e,	0x9b18,	0x9931,	0x975a,	0x9593,	0x93dc,
		0x9236,	0x90a1,	0x8f1e,	0x8dab,	0x8c4b,	0x8afc,	0x89bf,	0x8894,
		0x877c,	0x8676,	0x8583,	0x84a3,	0x83d7,	0x831d,	0x8276,	0x81e3,
		0x8163,	0x80f7,	0x809e,	0x8059,	0x8028,	0x800a,	0x6488,	0x0080,
		0x03ff,	0x0116,	0x0002,	0x0080,	0x4000,	0x3fd7,	0x3faf,	0x3f86,
		0x3f5d,	0x3f34,	0x3f0c,	0x3ee3,	0x3eba,	0x3e91,	0x3e68,	0x3e40,
		0x3e17,	0x3dee,	0x3dc5,	0x3d9c,	0x3d74,	0x3d4b,	0x3d22,	0x3cf9,
		0x3cd0,	0x3ca7,	0x3c7f,	0x3c56,	0x3c2d,	0x3c04,	0x3bdb,	0x3bb2,
		0x3b89,	0x3b60,	0x3b37,	0x3b0e,	0x3ae5,	0x3abc,	0x3a93,	0x3a69,
		0x3a40,	0x3a17,	0x39ee,	0x39c5,	0x399c,	0x3972,	0x3949,	0x3920,
		0x38f6,	0x38cd,	0x38a4,	0x387a,	0x3851,	0x3827,	0x37fe,	0x37d4,
		0x37aa,	0x3781,	0x3757,	0x372d,	0x3704,	0x36da,	0x36b0,	0x3686,
		0x365c,	0x3632,	0x3609,	0x35df,	0x35b4,	0x358a,	0x3560,	0x3536,
		0x350c,	0x34e1,	0x34b7,	0x348d,	0x3462,	0x3438,	0x340d,	0x33e3,
		0x33b8,	0x338d,	0x3363,	0x3338,	0x330d,	0x32e2,	0x32b7,	0x328c,
		0x3261,	0x3236,	0x320b,	0x31df,	0x31b4,	0x3188,	0x315d,	0x3131,
		0x3106,	0x30da,	0x30ae,	0x3083,	0x3057,	0x302b,	0x2fff,	0x2fd2,
		0x2fa6,	0x2f7a,	0x2f4d,	0x2f21,	0x2ef4,	0x2ec8,	0x2e9b,	0x2e6e,
		0x2e41,	0x2e14,	0x2de7,	0x2dba,	0x2d8d,	0x2d60,	0x2d32,	0x2d05,
		0x2cd7,	0x2ca9,	0x2c7b,	0x2c4d,	0x2c1f,	0x2bf1,	0x2bc3,	0x2b94,
		0x2b66,	0x2b37,	0x2b09,	0x2ada,	0x2aab,	0x2a7c,	0x2a4c,	0x2a1d,
		0x29ed,	0x29be,	0x298e,	0x295e,	0x292e,	0x28fe,	0x28ce,	0x289d,
		0x286d,	0x283c,	0x280b,	0x27da,	0x27a9,	0x2777,	0x2746,	0x2714,
		0x26e2,	0x26b0,	0x267e,	0x264c,	0x2619,	0x25e7,	0x25b4,	0x2581,
		0x254d,	0x251a,	0x24e6,	0x24b2,	0x247e,	0x244a,	0x2415,	0x23e1,
		0x23ac,	0x2376,	0x2341,	0x230b,	0x22d6,	0x229f,	0x2269,	0x2232,
		0x21fc,	0x21c4,	0x218d,	0x2155,	0x211d,	0x20e5,	0x20ad,	0x2074,
		0x203b,	0x2001,	0x1fc7,	0x1f8d,	0x1f53,	0x1f18,	0x1edd,	0x1ea1,
		0x1e66,	0x1e29,	0x1ded,	0x1db0,	0x1d72,	0x1d35,	0x1cf6,	0x1cb8,
		0x1c79,	0x1c39,	0x1bf9,	0x1bb8,	0x1b77,	0x1b36,	0x1af4,	0x1ab1,
		0x1a6e,	0x1a2a,	0x19e6,	0x19a1,	0x195c,	0x1915,	0x18ce,	0x1887,
		0x183f,	0x17f5,	0x17ac,	0x1761,	0x1715,	0x16c9,	0x167c,	0x162e,
		0x15df,	0x158e,	0x153d,	0x14eb,	0x1497,	0x1442,	0x13ec,	0x1395,
		0x133c,	0x12e2,	0x1286,	0x1228,	0x11c9,	0x1167,	0x1104,	0x109e,
		0x1036,	0x0fcc,	0x0f5f,	0x0eef,	0x0e7b,	0x0e04,	0x0d89,	0x0d0a,
		0x0c86,	0x0bfd,	0x0b6d,	0x0ad6,	0x0a36,	0x098d,	0x08d7,	0x0811,
		0x0736,	0x063e,	0x0519,	0x039a,	0x0000,	0x7fff,	0x0100,	0x0080,
		0x021d,	0x00c8,	0x00ce,	0x0048,	0x0a26,	0x277a,	0x00ce,	0x6488,
		0x14ac,	0x0001,	0x00f9,	0x00fc,	0x00ff,	0x00fc,	0x00f9,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,
		0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff,	0xffff};

	private static final int DSP1_MulTable[] = {
		0x0000,  0x0003,  0x0006,  0x0009,  0x000c,  0x000f,  0x0012,  0x0015,
		0x0019,  0x001c,  0x001f,  0x0022,  0x0025,  0x0028,  0x002b,  0x002f,
		0x0032,  0x0035,  0x0038,  0x003b,  0x003e,  0x0041,  0x0045,  0x0048,
		0x004b,  0x004e,  0x0051,  0x0054,  0x0057,  0x005b,  0x005e,  0x0061,
		0x0064,  0x0067,  0x006a,  0x006d,  0x0071,  0x0074,  0x0077,  0x007a,
		0x007d,  0x0080,  0x0083,  0x0087,  0x008a,  0x008d,  0x0090,  0x0093,
		0x0096,  0x0099,  0x009d,  0x00a0,  0x00a3,  0x00a6,  0x00a9,  0x00ac,
		0x00af,  0x00b3,  0x00b6,  0x00b9,  0x00bc,  0x00bf,  0x00c2,  0x00c5,
		0x00c9,  0x00cc,  0x00cf,  0x00d2,  0x00d5,  0x00d8,  0x00db,  0x00df,
		0x00e2,  0x00e5,  0x00e8,  0x00eb,  0x00ee,  0x00f1,  0x00f5,  0x00f8,
		0x00fb,  0x00fe,  0x0101,  0x0104,  0x0107,  0x010b,  0x010e,  0x0111,
		0x0114,  0x0117,  0x011a,  0x011d,  0x0121,  0x0124,  0x0127,  0x012a,
		0x012d,  0x0130,  0x0133,  0x0137,  0x013a,  0x013d,  0x0140,  0x0143,
		0x0146,  0x0149,  0x014d,  0x0150,  0x0153,  0x0156,  0x0159,  0x015c,
		0x015f,  0x0163,  0x0166,  0x0169,  0x016c,  0x016f,  0x0172,  0x0175,
		0x0178,  0x017c,  0x017f,  0x0182,  0x0185,  0x0188,  0x018b,  0x018e,
		0x0192,  0x0195,  0x0198,  0x019b,  0x019e,  0x01a1,  0x01a4,  0x01a8,
		0x01ab,  0x01ae,  0x01b1,  0x01b4,  0x01b7,  0x01ba,  0x01be,  0x01c1,
		0x01c4,  0x01c7,  0x01ca,  0x01cd,  0x01d0,  0x01d4,  0x01d7,  0x01da,
		0x01dd,  0x01e0,  0x01e3,  0x01e6,  0x01ea,  0x01ed,  0x01f0,  0x01f3,
		0x01f6,  0x01f9,  0x01fc,  0x0200,  0x0203,  0x0206,  0x0209,  0x020c,
		0x020f,  0x0212,  0x0216,  0x0219,  0x021c,  0x021f,  0x0222,  0x0225,
		0x0228,  0x022c,  0x022f,  0x0232,  0x0235,  0x0238,  0x023b,  0x023e,
		0x0242,  0x0245,  0x0248,  0x024b,  0x024e,  0x0251,  0x0254,  0x0258,
		0x025b,  0x025e,  0x0261,  0x0264,  0x0267,  0x026a,  0x026e,  0x0271,
		0x0274,  0x0277,  0x027a,  0x027d,  0x0280,  0x0284,  0x0287,  0x028a,
		0x028d,  0x0290,  0x0293,  0x0296,  0x029a,  0x029d,  0x02a0,  0x02a3,
		0x02a6,  0x02a9,  0x02ac,  0x02b0,  0x02b3,  0x02b6,  0x02b9,  0x02bc,
		0x02bf,  0x02c2,  0x02c6,  0x02c9,  0x02cc,  0x02cf,  0x02d2,  0x02d5,
		0x02d8,  0x02db,  0x02df,  0x02e2,  0x02e5,  0x02e8,  0x02eb,  0x02ee,
		0x02f1,  0x02f5,  0x02f8,  0x02fb,  0x02fe,  0x0301,  0x0304,  0x0307,
		0x030b,  0x030e,  0x0311,  0x0314,  0x0317,  0x031a,  0x031d,  0x0321};

	private static final int DSP1_SinTable[] = {
		 0x0000,  0x0324,  0x0647,  0x096a,  0x0c8b,  0x0fab,  0x12c8,  0x15e2,
		 0x18f8,  0x1c0b,  0x1f19,  0x2223,  0x2528,  0x2826,  0x2b1f,  0x2e11,
		 0x30fb,  0x33de,  0x36ba,  0x398c,  0x3c56,  0x3f17,  0x41ce,  0x447a,
		 0x471c,  0x49b4,  0x4c3f,  0x4ebf,  0x5133,  0x539b,  0x55f5,  0x5842,
		 0x5a82,  0x5cb4,  0x5ed7,  0x60ec,  0x62f2,  0x64e8,  0x66cf,  0x68a6,
		 0x6a6d,  0x6c24,  0x6dca,  0x6f5f,  0x70e2,  0x7255,  0x73b5,  0x7504,
		 0x7641,  0x776c,  0x7884,  0x798a,  0x7a7d,  0x7b5d,  0x7c29,  0x7ce3,
		 0x7d8a,  0x7e1d,  0x7e9d,  0x7f09,  0x7f62,  0x7fa7,  0x7fd8,  0x7ff6,
		 0x7fff,  0x7ff6,  0x7fd8,  0x7fa7,  0x7f62,  0x7f09,  0x7e9d,  0x7e1d,
		 0x7d8a,  0x7ce3,  0x7c29,  0x7b5d,  0x7a7d,  0x798a,  0x7884,  0x776c,
		 0x7641,  0x7504,  0x73b5,  0x7255,  0x70e2,  0x6f5f,  0x6dca,  0x6c24,
		 0x6a6d,  0x68a6,  0x66cf,  0x64e8,  0x62f2,  0x60ec,  0x5ed7,  0x5cb4,
		 0x5a82,  0x5842,  0x55f5,  0x539b,  0x5133,  0x4ebf,  0x4c3f,  0x49b4,
		 0x471c,  0x447a,  0x41ce,  0x3f17,  0x3c56,  0x398c,  0x36ba,  0x33de,
		 0x30fb,  0x2e11,  0x2b1f,  0x2826,  0x2528,  0x2223,  0x1f19,  0x1c0b,
		 0x18f8,  0x15e2,  0x12c8,  0x0fab,  0x0c8b,  0x096a,  0x0647,  0x0324,
		-0x0000, -0x0324, -0x0647, -0x096a, -0x0c8b, -0x0fab, -0x12c8, -0x15e2,
		-0x18f8, -0x1c0b, -0x1f19, -0x2223, -0x2528, -0x2826, -0x2b1f, -0x2e11,
		-0x30fb, -0x33de, -0x36ba, -0x398c, -0x3c56, -0x3f17, -0x41ce, -0x447a,
		-0x471c, -0x49b4, -0x4c3f, -0x4ebf, -0x5133, -0x539b, -0x55f5, -0x5842,
		-0x5a82, -0x5cb4, -0x5ed7, -0x60ec, -0x62f2, -0x64e8, -0x66cf, -0x68a6,
		-0x6a6d, -0x6c24, -0x6dca, -0x6f5f, -0x70e2, -0x7255, -0x73b5, -0x7504,
		-0x7641, -0x776c, -0x7884, -0x798a, -0x7a7d, -0x7b5d, -0x7c29, -0x7ce3,
		-0x7d8a, -0x7e1d, -0x7e9d, -0x7f09, -0x7f62, -0x7fa7, -0x7fd8, -0x7ff6,
		-0x7fff, -0x7ff6, -0x7fd8, -0x7fa7, -0x7f62, -0x7f09, -0x7e9d, -0x7e1d,
		-0x7d8a, -0x7ce3, -0x7c29, -0x7b5d, -0x7a7d, -0x798a, -0x7884, -0x776c,
		-0x7641, -0x7504, -0x73b5, -0x7255, -0x70e2, -0x6f5f, -0x6dca, -0x6c24,
		-0x6a6d, -0x68a6, -0x66cf, -0x64e8, -0x62f2, -0x60ec, -0x5ed7, -0x5cb4,
		-0x5a82, -0x5842, -0x55f5, -0x539b, -0x5133, -0x4ebf, -0x4c3f, -0x49b4,
		-0x471c, -0x447a, -0x41ce, -0x3f17, -0x3c56, -0x398c, -0x36ba, -0x33de,
		-0x30fb, -0x2e11, -0x2b1f, -0x2826, -0x2528, -0x2223, -0x1f19, -0x1c0b,
		-0x18f8, -0x15e2, -0x12c8, -0x0fab, -0x0c8b, -0x096a, -0x0647, -0x0324};

	private static final int MaxAZS_Exp[] = {
		0x38b4, 0x38b7, 0x38ba, 0x38be, 0x38c0, 0x38c4, 0x38c7, 0x38ca,
		0x38ce,	0x38d0, 0x38d4, 0x38d7, 0x38da, 0x38dd, 0x38e0, 0x38e4
	};
	
	void InitDSP1()
	{
	    if (!init)
	    {
	        init = true;
	    }
	}
	
	void ResetDSP1 ()
	{		
		InitDSP1 ();
		
		waiting4command = true;
		in_count = 0;
		out_count = 0;
		in_index = 0;
		out_index = 0;
		first_parameter = true;
	
		dsp4.waiting4command = true;
	}
	
	
	int GetDSP (int address)
	{
		return DSP1GetByte(address);
	}

	void SetDSP (int value, int address)
	{
		DSP1SetByte(value, address);
	}

	private void DSP1SetByte(int value, int address)
	{	
	    if (address < boundary)
	    {
			if( ( command == 0x0A || command == 0x1A ) && out_count !=0 )
			{
				out_count--;
				out_index++;
				return;
			}
			else if (waiting4command)
			{
				command = value;
				in_index = 0;
				waiting4command = false;
				first_parameter = true;

				switch (value)
				{
				case 0x00: in_count = 2;	break;
				case 0x30:
				case 0x10: in_count = 2;	break;
				case 0x20: in_count = 2;	break;
				case 0x24:
				case 0x04: in_count = 2;	break;
				case 0x08: in_count = 3;	break;
				case 0x18: in_count = 4;	break;
				case 0x28: in_count = 3;	break;
				case 0x38: in_count = 4;	break;
				case 0x2c:
				case 0x0c: in_count = 3;	break;
				case 0x3c:
				case 0x1c: in_count = 6;	break;
				case 0x32:
				case 0x22:
				case 0x12:
				case 0x02: in_count = 7;	break;
				case 0x0a: in_count = 1;	break;
				case 0x3a:
				case 0x2a:
				case 0x1a:
					 command =0x1a;
					in_count = 1;	break;
				case 0x16:
				case 0x26:
				case 0x36:
				case 0x06: in_count = 3;	break;
				case 0x1e:
				case 0x2e:
				case 0x3e:
				case 0x0e: in_count = 2;	break;
				case 0x05:
				case 0x35:
				case 0x31:
				case 0x01: in_count = 4;	break;
				case 0x15:
				case 0x11: in_count = 4;	break;
				case 0x25:
				case 0x21: in_count = 4;	break;
				case 0x09:
				case 0x39:
				case 0x3d:
				case 0x0d: in_count = 3;	break;
				case 0x19:
				case 0x1d: in_count = 3;	break;
				case 0x29:
				case 0x2d: in_count = 3;	break;
				case 0x33:
				case 0x03: in_count = 3;	break;
				case 0x13: in_count = 3;	break;
				case 0x23: in_count = 3;	break;
				case 0x3b:
				case 0x0b: in_count = 3;	break;
				case 0x1b: in_count = 3;	break;
				case 0x2b: in_count = 3;	break;
				case 0x34:
				case 0x14: in_count = 6;	break;
				case 0x07:
				case 0x0f: in_count = 1;	break;
				case 0x27:
				case 0x2F: in_count=1; break;
				case 0x17:
				case 0x37:
				case 0x3F:
					command=0x1f;
				case 0x1f: in_count = 1;	break;
				default:

				case 0x80:
					in_count = 0;
					waiting4command = true;
					first_parameter = true;
					break;
				}
				in_count<<=1;
			}
			else
			{
				parameters.put8Bit(in_index, value);
				first_parameter = false;
				in_index++;
			}

			if (waiting4command ||
				(first_parameter && value == 0x80))
			{
				waiting4command = true;
				first_parameter = false;
			}
			else if(first_parameter && (in_count != 0 || (in_count==0&&in_index==0)))
			{
			}

			else
			{
				if (in_count > 0)
				{
					if (--in_count == 0)
					{
						// Actually execute the command
						waiting4command = true;
						out_index = 0;
						switch (command)
						{
						case 0x1f:
							out_count=2048;
							break;
						case 0x00: DSPOp00(); break;

						case 0x20: DSPOp20(); break;

						case 0x30:
						case 0x10: DSPOp10(); break;

						case 0x24:
						case 0x04: DSPOp04(); break;

						case 0x08: DSPOp08(); break;

						case 0x18: DSPOp18(); break;

						case 0x38: DSPOp38(); break;

						case 0x28: DSPOp28(); break;

						case 0x2c:
						case 0x0c: DSPOp0C(); break;

						case 0x3c:
						case 0x1c: DSPOp1C(); break;

						case 0x32:
						case 0x22:
						case 0x12:
						case 0x02: DSPOp02(); break;

						case 0x3a:
						case 0x2a:
						case 0x1a:
						case 0x0a: DSPOp0A(); break;

						case 0x16:
						case 0x26:
						case 0x36:
						case 0x06: DSPOp06(); break;

						case 0x1e:
						case 0x2e:
						case 0x3e:
						case 0x0e: DSPOp0E(); break;

						case 0x05:
						case 0x35:
						case 0x31:
						case 0x01: DSPOp01(); break;

						case 0x15:
						case 0x11: DSPOp11(); break;

						case 0x25:
						case 0x21: DSPOp21(); break;

						case 0x09:
						case 0x39:
						case 0x3d:
						case 0x0d: DSPOp0D(); break;

						case 0x19:
						case 0x1d: DSPOp1D(); break;

						case 0x29:
						case 0x2d: DSPOp2D(); break;

						case 0x33:
						case 0x03: DSPOp03(); break;

						case 0x13: DSPOp13(); break;

						case 0x23: DSPOp23(); break;

						case 0x3b:
						case 0x0b: DSPOp0B(); break;

						case 0x1b: DSPOp1B(); break;

						case 0x2b: DSPOp2B(); break;

						case 0x34:
						case 0x14: DSPOp14(); break;

						case 0x27:
						case 0x2F: DSPOp2F(); break;


						case 0x07:
						case 0x0F: DSPOp0F(); break;

						default:
							break;
						}
					}
				}
			}
	    }
	}
	
	private int DSP1GetByte(int address)
	{		
	    int t;
	    if (address < boundary)
	    {
            if (out_count > 0)
            {
                t = output.get8Bit(out_index);

                out_index++;
                if (--out_count == 0)
                {
					if (command == 0x1a || command == 0x0a)
					{
						DSPOp0A ();
						out_count = 8;
						out_index = 0;

						output.put8Bit(0, A & 0xFF);
						output.put8Bit(1, B & 0xFF);
						output.put8Bit(2, B & 0xFF);
						output.put8Bit(3, ( B >> 8 ) & 0xFF);
						output.put8Bit(4, C & 0xFF);
						output.put8Bit(5, ( C >> 8 ) & 0xFF);
						output.put8Bit(6, D & 0xFF);
						output.put8Bit(7, ( D >> 8 ) & 0xFF);

					}
					if(command==0x1f)
					{
						if( (out_index % 2) != 0)
						{
							t = (byte) DSP1ROM[out_index>>1];
						}
						else
						{
						    t=DSP1ROM[out_index>>1]>>8;
						}
					}
                }
                waiting4command = true;
            }
            else
            {
            	t = 0xff;
            }
	    }
	    else t = 0x80;

	    return t;
	}

	private final void DSPOp00()
	{		
		int Multiplicand = parameters.get8Bit(0) | (parameters.get8Bit(1) << 8);
		int Multiplier = parameters.get8Bit(2) | (parameters.get8Bit(3) << 8);
		
		int Result = Multiplicand * Multiplier >> 15;
		
		out_count = 2;
		output.put8Bit(0, Result & 0xFF);
		output.put8Bit(1, (Result >> 8) & 0xFF);
	}
	
	private final void DSPOp20()
	{
		int Multiplicand = parameters.get8Bit(0) | (parameters.get8Bit(1) << 8);
		int Multiplier = parameters.get8Bit(2) | (parameters.get8Bit(3) << 8);
		
		int Result = Multiplicand * Multiplier >> 15;
		Result++;
		
		out_count = 2;
		output.put8Bit(0, Result & 0xFF);
		output.put8Bit(1, (Result >> 8) & 0xFF);
	}
	
	private final void DSPOp04 ()
	{
		int Angle = parameters.get8Bit(0) | (parameters.get8Bit(1) << 8);
		int Radius = parameters.get8Bit(2) | (parameters.get8Bit(3) << 8);
		
		int Sin = DSP1_Sin(Angle) * Radius >> 15;
		int Cos = DSP1_Cos(Angle) * Radius >> 15;
		
		out_count = 4;
		output.put8Bit(0, Sin & 0xFF);
		output.put8Bit(1, (Sin >> 8) & 0xFF);
		output.put8Bit(2, Cos & 0xFF);
		output.put8Bit(3, ( Cos >> 8 ) & 0xFF );
	}
	
	private final void DSPOp10()
	{
		int Coefficient = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8 );
		int Exponent = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8 );

		int[] result = DSP1_Inverse(Coefficient, Exponent);
		int iCoefficient = result[0];
		int iExponent = result[1];
		
		out_count = 4;
		output.put8Bit(0, iCoefficient & 0xFF);
		output.put8Bit(1, ( iCoefficient >> 8 ) & 0xFF);
		output.put8Bit(2, iExponent & 0xff);
		output.put8Bit(3, ( iExponent >> 8 ) & 0xff );
	}
	
	private final void DSPOp08()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int Size = (X * X + Y * Y + Z * Z) << 1;
		int Ll = Size & 0xffff;
		int Lh = (Size >> 16) & 0xffff;

		out_count = 4;
		output.put8Bit(0, Ll & 0xFF);
		output.put8Bit(1, ( Ll>> 8 ) & 0xFF);
		output.put8Bit(2, Lh & 0xFF);
		output.put8Bit(3, ( Lh >> 8 ) & 0xFF);
	}
	
	private final void DSPOp18()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int R = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
	
		int D = (X * X + Y * Y + Z * Z - R * R) >> 15;
	
		out_count = 2;
		output.put8Bit(0, D & 0xFF);
		output.put8Bit(1, ( D >> 8 ) & 0xFF);
	}
	
	private final void DSPOp38()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int R = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
	
		int D = (X * X + Y * Y + Z * Z - R * R) >> 15;
		D++;
	
		out_count = 2;
		output.put8Bit(0, D & 0xFF);
		output.put8Bit(1, ( D >> 8 ) & 0xFF);
	}
	
	private final void DSPOp28()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int R;
	
		int Radius = X * X + Y * Y + Z * Z;

		if (Radius == 0)
		{
			R = 0;
		}
		else
		{
			int C, E, Pos, Node1, Node2;
			
			int[] normalized = DSP1_NormalizeDouble(Radius);
			C = normalized[0];
			E = normalized[1];
			
			if ( ( E & 1 ) > 0 )
			{
				C = C * 0x4000 >> 15;
			}

			Pos = C * 0x0040 >> 15;

			Node1 = DSP1ROM[0x00d5 + Pos];
			Node2 = DSP1ROM[0x00d6 + Pos];

			R = ((Node2 - Node1) * (C & 0x1ff) >> 9) + Node1;
			R >>= (E >> 1);
		}
		
		out_count = 2;
		output.put8Bit(0, R & 0xFF);
		output.put8Bit(1, ( R >> 8 ) & 0xFF);
	}
	
	private final void DSPOp0C()
	{
		int A = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int X1 = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Y1 = parameters.get8Bit(5) | ( parameters.get8Bit(5) << 8);
		
		int X2 = (Y1 * DSP1_Sin(A) >> 15) + (X1 * DSP1_Cos(A) >> 15);
		int Y2 = (Y1 * DSP1_Cos(A) >> 15) - (X1 * DSP1_Sin(A) >> 15);
		
		out_count = 4;
		output.put8Bit(0, X2 & 0xFF );
		output.put8Bit(1, ( X2 >> 8 ) & 0xFF );
		output.put8Bit(2, Y2 & 0xFF );
		output.put8Bit(3, ( Y2 >> 8 ) & 0xFF );
	}
	
	private final void DSPOp1C()
	{
		int Z = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int X = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int XBR = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
		int YBR = parameters.get8Bit(8) | ( parameters.get8Bit(9) << 8);
		int ZBR = parameters.get8Bit(10) | ( parameters.get8Bit(11) << 8);
	
		// Rotate Around Z1
		int X1 = (YBR * DSP1_Sin(Z) >> 15) + (XBR * DSP1_Cos(Z) >> 15);
		int Y1 = (YBR * DSP1_Cos(Z) >> 15) - (XBR * DSP1_Sin(Z) >> 15);
		XBR = X1;
		YBR = Y1;

		// Rotate Around Y1
		int Z1 = (XBR * DSP1_Sin(Y) >> 15) + (ZBR * DSP1_Cos(Y) >> 15);
		X1 = (XBR * DSP1_Cos(Y) >> 15) - (ZBR * DSP1_Sin(Y) >> 15);
		int XAR = X1;
		ZBR = Z1;

		// Rotate Around X1
		Y1 = (ZBR * DSP1_Sin(X) >> 15) + (YBR * DSP1_Cos(X) >> 15);
		Z1 = (ZBR * DSP1_Cos(X) >> 15) - (YBR * DSP1_Sin(X) >> 15);
		int YAR = Y1; 
		int ZAR = Z1;
	
		out_count = 6;
		output.put8Bit(0, XAR & 0xFF );
		output.put8Bit(1, ( XAR>>8) & 0xFF );
		output.put8Bit(2, YAR & 0xFF );
		output.put8Bit(3, ( YAR >> 8 ) & 0xFF );
		output.put8Bit(4, ZAR & 0xFF );
		output.put8Bit(5, ( ZAR >> 8 ) & 0xFF );
	}
	
	private final void DSPOp02()
	{
		int[] result;
		
		int Fx = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Fy = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Fz = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int Lfe = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
		int Les = parameters.get8Bit(8) | ( parameters.get8Bit(9) << 8);
		int Aas = parameters.get8Bit(10) | ( parameters.get8Bit(11) << 8);
		int Azs = parameters.get8Bit(12) | ( parameters.get8Bit(13) << 8);
	
		int CSec, C, E, MaxAZS, Aux;
		int LfeNx, LfeNy, LfeNz;
		int LesNx, LesNy, LesNz;
		
		// Copy Zenith angle for clipping
		int AZS = Azs;
		
		// Store Sine and Cosine of Azimuth and Zenith angle
		SinAas = DSP1_Sin(Aas);
		CosAas = DSP1_Cos(Aas);
		SinAzs = DSP1_Sin(Azs);
		CosAzs = DSP1_Cos(Azs);
		
		Nx = SinAzs * -SinAas >> 15;
		Ny = SinAzs * CosAas >> 15;
		Nz = CosAzs * 0x7fff >> 15;
		
		LfeNx = Lfe * Nx >> 15;
		LfeNy = Lfe * Ny >> 15;
		LfeNz = Lfe * Nz >> 15;
		
		// Center of Projection
		CentreX = Fx + LfeNx;
		CentreY = Fy + LfeNy;
		int CentreZ = Fz + LfeNz;
		
		LesNx = Les * Nx >> 15;
		LesNy = Les * Ny >> 15;
		LesNz = Les * Nz >> 15;
		
		Gx = CentreX - LesNx;
		Gy = CentreY - LesNy;
		Gz = CentreZ - LesNz;
		
		result = DSP1_Normalize(Les, 0);
		C_Les = result[0];
		E_Les = result[1];
		G_Les = Les;
		
		result = DSP1_Normalize(CentreZ, 0);
		C = result[0];
		E = result[1];
		
		VPlane_C = C;
		VPlane_E = E;
		
		// Determine clip boundary and clip Zenith angle if necessary
		MaxAZS = MaxAZS_Exp[-E];
		
		if (AZS < 0)
		{
			MaxAZS = -MaxAZS;
			
			if (AZS < MaxAZS + 1)
			{
				AZS = MaxAZS + 1;
			}
			
		}
		else if (AZS > MaxAZS)
		{
			AZS = MaxAZS;
		}
		
		// Store Sine and Cosine of clipped Zenith angle
		SinAZS = DSP1_Sin(AZS);
		CosAZS = DSP1_Cos(AZS);
		
		result = DSP1_Inverse(CosAZS, 0);
		SecAZS_C1 = result[0];
		SecAZS_E1 = result[1];
		
		
		result = DSP1_Normalize(C * SecAZS_C1 >> 15, E);
		C = result[0];
		E = result[1];
		
		E += SecAZS_E1;
		
		C = DSP1_Truncate(C, E) * SinAZS >> 15;
		
		CentreX += C * SinAas >> 15;
		CentreY -= C * CosAas >> 15;
		
		int Cx = CentreX;
		int Cy = CentreY;
		
		// Raster number of imaginary center and horizontal line
		int Vof = 0;
		
		if ((Azs != AZS) || (Azs == MaxAZS))
		{
			if (Azs == -32768) Azs = -32767;
		
			C = Azs - MaxAZS;
			if (C >= 0) C--;
			Aux = ~(C << 2);
			
			C = Aux * DSP1ROM[0x0328] >> 15;
			C = (C * Aux >> 15) + DSP1ROM[0x0327];
			Vof -= (C * Aux >> 15) * Les >> 15;
			
			C = Aux * Aux >> 15;
			Aux = (C * DSP1ROM[0x0324] >> 15) + DSP1ROM[0x0325];
			CosAZS += (C * Aux >> 15) * CosAZS >> 15;
		}
		
		VOffset = Les * CosAZS >> 15;
		
		result = DSP1_Inverse(SinAZS, 0);
		CSec = result[0];
		E = result[1];
		
		result = DSP1_Normalize(VOffset, E);
		C = result[0];
		E = result[1];
		
		result = DSP1_Normalize(C * CSec >> 15, E);
		C = result[0];
		E = result[1];
		
		if (C == -32768) { C >>= 1; E++; }
		
		int Vva = DSP1_Truncate(-C, E);
		
		// Store Secant of clipped Zenith angle
		result = DSP1_Inverse(CosAZS, 0);
		SecAZS_C2 = result[0];
		SecAZS_E2 = result[0];
			
		out_count = 8;
		output.put8Bit(0, Vof & 0xFF);
		output.put8Bit(1, ( Vof >> 8 ) & 0xFF);
		output.put8Bit(2, Vva & 0xFF);
		output.put8Bit(3, ( Vva >> 8 ) & 0xFF);
		output.put8Bit(4, Cx & 0xFF);
		output.put8Bit(5, ( Cx >> 8 ) & 0xFF);
		output.put8Bit(6, Cy & 0xFF);
		output.put8Bit(7, ( Cy >> 8 ) & 0xFF);
	}

	private final void DSPOp0A()
	{
		DSP1_Raster();
	
		out_count = 8;
		output.put8Bit(0, A & 0xFF);
		output.put8Bit(2, B & 0xFF);
		output.put8Bit(4, C & 0xFF);
		output.put8Bit(6, D & 0xFF);
		output.put8Bit(1, ( A >> 8 ) & 0xFF);
		output.put8Bit(3, ( B >> 8 ) & 0xFF);
		output.put8Bit(5, ( C >> 8 ) & 0xFF);
		output.put8Bit(7, ( D >> 8 ) & 0xFF);
		in_index=0;
	}
	
	private final void DSP1_Raster()
	{
		int Vs = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);

		int C1, E1, C2, E2;
		int[] result;

		result = DSP1_Inverse((Vs * SinAzs >> 15) + VOffset, 7);
		C1 = result[0];
		E1 = result[1];
		
		E1 += VPlane_E;

		C2 = C1 * VPlane_C >> 15;
		E2 = E1 + SecAZS_E2;

		result = DSP1_Normalize(C2, E1);
		C1 = result[0];
		E1 = result[1];

		C1 = DSP1_Truncate(C1, E1);

		A = C1 * CosAas >> 15;
		C = C1 * SinAas >> 15;

		result = DSP1_Normalize(C2 * SecAZS_C2 >> 15, E2);
		C1 = result[0];
		E2 = result[1];

		C1 = DSP1_Truncate(C1, E2);

		B = C1 * -SinAas >> 15;
		D = C1 * CosAas >> 15;

		Vs++;
	}

	private final void DSPOp06()
	{
		int[] result;
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		
		int H, V, M;
		int aux, aux4;
		int E, E2, E3, E4, refE, E6, E7;
		int C2, C4, C6, C8, C9, C10, C11, C12, C16, C17, C18, C19, C20, C21, C22, C23, C24, C25, C26;
		int Px, Py, Pz;
		
		E4 = E3 = E2 = E = 0;

		result = DSP1_NormalizeDouble(X - Gx);
		Px = result[0];
		E4 = result[1];
		
		result = DSP1_NormalizeDouble(Y - Gy);
		Py = result[0];
		E = result[1];
		
		result = DSP1_NormalizeDouble(Z - Gz);
		Pz = result[0];
		E3 = result[1];

		Px = Px >> 1; E4--;   // to avoid overflows when calculating the scalar products
		Py = Py >> 1; E--;
		Pz = Pz >> 1; E3--;

		refE = ( E < E3 ) ? E : E3;
		refE = ( refE < E4 ) ? refE : E4;

		Px = DSP1_ShiftR( Px, E4 - refE);    // normalize them to the same exponent
		Py = DSP1_ShiftR( Py, E - refE);
		Pz = DSP1_ShiftR( Pz, E3 - refE);

		C11 =- (Px * Nx >> 15);
		C8 =- (Py * Ny >> 15);
		C9 =- (Pz * Nz >> 15);
		C12 = C11 + C8 + C9;   // this cannot overflow!

		aux4 = C12;   // de-normalization with 32-bits arithmetic
		refE = 16 - refE;    // refE can be up to 3
		  
		if (refE >= 0)
		{
		  aux4 = aux4 << refE;
		}
		else
		{
			aux4 = aux4 >> -(refE);
		}
		
		if (aux4 == -1)
		{
			aux4 = 0;      // why?
		}
		
		aux4 = aux4 >> 1;

		aux = G_Les + aux4;   // Les - the scalar product of P with the normal vector of the screen
		result = DSP1_NormalizeDouble(aux);
		C10 = result[0];
		E2 = result[1];
		
		E2 = 15-E2;

		result = DSP1_Inverse(C10, 0);
		C4 = result[0];
		E4 = result[1];
		 
		C2 = C4 * C_Les >> 15;                 // scale factor

		// H
		E7 = 0;
		C16 = (Px * (CosAas * 0x7fff >> 15) >> 15);
		C20 = (Py * (SinAas * 0x7fff >> 15) >> 15);
		C17 = C16 + C20;   // scalar product of P with the normalized horizontal vector of the screen...

		C18 = C17 * C2 >> 15;    // ... multiplied by the scale factor
		result = DSP1_Normalize(C18, E7);
		C19 = result[0];
		E7 = result[1];
		
		H = DSP1_Truncate(C19, E_Les - E2 + refE + E7);

		// V
		E6 = 0;
		C21 = Px * (CosAzs * -SinAas >> 15) >> 15;
		C22 = Py * (CosAzs * CosAas >> 15 )>> 15;
		C23 = Pz * (-SinAzs * 0x7fff >> 15 ) >> 15;
		C24 = C21 + C22 + C23;   // scalar product of P with the normalized vertical vector of the screen...

		C26 = C24 * C2 >> 15;    // ... multiplied by the scale factor
		result = DSP1_Normalize(C26, E6);
		C25 = result[0];
		E6 = result[1];
		
		V = DSP1_Truncate(C25, E_Les - E2 + refE + E6);

		// M
		result = DSP1_Normalize(C2, E4);
		C6 = result[0];
		E4 = result[1];
		
		M = DSP1_Truncate(C6, E4+E_Les-E2-7); // M is the scale factor divided by 2^7
		
		out_count = 6;
		output.put8Bit(0, H & 0xff);
		output.put8Bit(1, ( H >> 8 ) & 0xFF);
		output.put8Bit(2, V & 0xFF );
		output.put8Bit(3, ( V >> 8 ) & 0xFF);
		output.put8Bit(4, M & 0xFF);
		output.put8Bit(5, ( M >> 8 ) & 0xFF);
	}

	private final void DSPOp0E()
	{
		int H = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int V = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		
		int X, Y;
		int C, E, C1, E1;
		int[] result;

		result = DSP1_Inverse((V * SinAzs >> 15) + VOffset, 8);
		C = result[0];
		E = result[1];
		
		E += VPlane_E;

		C1 = C * VPlane_C >> 15;
		E1 = E + SecAZS_E1;

		H <<= 8;

		result = DSP1_Normalize(C1, E);
		C = result[0];
		E = result[1];

		C = DSP1_Truncate(C, E) * H >> 15;

		X = CentreX + (C * CosAas >> 15);
		Y = CentreY - (C * SinAas >> 15);

		V = V << 8;

		result = DSP1_Normalize(C1 * SecAZS_C1 >> 15, E1);
		C = result[0];
		E1 = result[1];

		C = DSP1_Truncate(C, E1) * V >> 15;

		X += C * -SinAas >> 15;
		Y += C * CosAas >> 15;
		
		out_count = 4;
		output.put8Bit(0, X & 0xFF);
		output.put8Bit(1, ( X >> 8 ) & 0xFF);
		output.put8Bit(2, Y & 0xFF);
		output.put8Bit(3, ( Y >> 8 ) & 0xFF);
	}

	private final void DSPOp01()
	{
		int m = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Zr = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Yr = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int Xr = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
	
		int SinAz = DSP1_Sin(Zr);
		int CosAz = DSP1_Cos(Zr);
		int SinAy = DSP1_Sin(Yr);
		int CosAy = DSP1_Cos(Yr);
		int SinAx = DSP1_Sin(Xr);
		int CosAx = DSP1_Cos(Xr);

		m = m >> 1;

		matrixA[0][0] = (m * CosAz >> 15) * CosAy >> 15;
		matrixA[0][1] = -((m * SinAz >> 15) * CosAy >> 15);
		matrixA[0][2] = m * SinAy >> 15;

		matrixA[1][0] = ((m * SinAz >> 15) * CosAx >> 15) + (((m * CosAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixA[1][1] = ((m * CosAz >> 15) * CosAx >> 15) - (((m * SinAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixA[1][2] = -((m * SinAx >> 15) * CosAy >> 15);

		matrixA[2][0] = ((m * SinAz >> 15) * SinAx >> 15) - (((m * CosAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixA[2][1] = ((m * CosAz >> 15) * SinAx >> 15) + (((m * SinAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixA[2][2] = (m * CosAx >> 15) * CosAy >> 15;
	}

	private final void DSPOp11()
	{
		int m = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Zr = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Yr = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int Xr = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
	
		int SinAz = DSP1_Sin(Zr);
		int CosAz = DSP1_Cos(Zr);
		int SinAy = DSP1_Sin(Yr);
		int CosAy = DSP1_Cos(Yr);
		int SinAx = DSP1_Sin(Xr);
		int CosAx = DSP1_Cos(Xr);
	
		m = m >> 1;
	
		matrixB[0][0] = (m * CosAz >> 15) * CosAy >> 15;
		matrixB[0][1] = -((m * SinAz >> 15) * CosAy >> 15);
		matrixB[0][2] = m * SinAy >> 15;
	
		matrixB[1][0] = ((m * SinAz >> 15) * CosAx >> 15) + (((m * CosAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixB[1][1] = ((m * CosAz >> 15) * CosAx >> 15) - (((m * SinAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixB[1][2] = -((m * SinAx >> 15) * CosAy >> 15);
	
		matrixB[2][0] = ((m * SinAz >> 15) * SinAx >> 15) - (((m * CosAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixB[2][1] = ((m * CosAz >> 15) * SinAx >> 15) + (((m * SinAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixB[2][2] = (m * CosAx >> 15) * CosAy >> 15;
	}
	
	private final void DSPOp21()
	{
		int m = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Zr = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Yr = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int Xr = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
	
		int SinAz = DSP1_Sin(Zr);
		int CosAz = DSP1_Cos(Zr);
		int SinAy = DSP1_Sin(Yr);
		int CosAy = DSP1_Cos(Yr);
		int SinAx = DSP1_Sin(Xr);
		int CosAx = DSP1_Cos(Xr);

		m = m >> 1;

		matrixC[0][0] = (m * CosAz >> 15) * CosAy >> 15;
		matrixC[0][1] = -((m * SinAz >> 15) * CosAy >> 15);
		matrixC[0][2] = m * SinAy >> 15;

		matrixC[1][0] = ((m * SinAz >> 15) * CosAx >> 15) + (((m * CosAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixC[1][1] = ((m * CosAz >> 15) * CosAx >> 15) - (((m * SinAz >> 15) * SinAx >> 15) * SinAy >> 15);
		matrixC[1][2] = -((m * SinAx >> 15) * CosAy >> 15);

		matrixC[2][0] = ((m * SinAz >> 15) * SinAx >> 15) - (((m * CosAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixC[2][1] = ((m * CosAz >> 15) * SinAx >> 15) + (((m * SinAz >> 15) * CosAx >> 15) * SinAy >> 15);
		matrixC[2][2] = (m * CosAx >> 15) * CosAy >> 15;
	}
	
	private final void DSPOp0D()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int F = (X * matrixA[0][0] >> 15) + (Y * matrixA[0][1] >> 15) + (Z * matrixA[0][2] >> 15);
		int L = (X * matrixA[1][0] >> 15) + (Y * matrixA[1][1] >> 15) + (Z * matrixA[1][2] >> 15);
		int U = (X * matrixA[2][0] >> 15) + (Y * matrixA[2][1] >> 15) + (Z * matrixA[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, F & 0xFF);
		output.put8Bit(1, ( F >> 8 ) & 0xFF);
		output.put8Bit(2, L & 0xFF);
		output.put8Bit(3, ( L >> 8 ) & 0xFF);
		output.put8Bit(4, U & 0xFF);
		output.put8Bit(5, ( U >> 8 ) & 0xFF);
	}
	
	private final void DSPOp1D()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int F = (X * matrixB[0][0] >> 15) + (Y * matrixB[0][1] >> 15) + (Z * matrixB[0][2] >> 15);
		int L = (X * matrixB[1][0] >> 15) + (Y * matrixB[1][1] >> 15) + (Z * matrixB[1][2] >> 15);
		int U = (X * matrixB[2][0] >> 15) + (Y * matrixB[2][1] >> 15) + (Z * matrixB[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, F & 0xFF);
		output.put8Bit(1, ( F >> 8 ) & 0xFF);
		output.put8Bit(2, L & 0xFF);
		output.put8Bit(3, ( L >> 8 ) & 0xFF);
		output.put8Bit(4, U & 0xFF);
		output.put8Bit(5, ( U >> 8 ) & 0xFF);
	}
	
	private final void DSPOp2D()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int F = (X * matrixC[0][0] >> 15) + (Y * matrixC[0][1] >> 15) + (Z * matrixC[0][2] >> 15);
		int L = (X * matrixC[1][0] >> 15) + (Y * matrixC[1][1] >> 15) + (Z * matrixC[1][2] >> 15);
		int U = (X * matrixC[2][0] >> 15) + (Y * matrixC[2][1] >> 15) + (Z * matrixC[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, F & 0xFF);
		output.put8Bit(1, ( F >> 8 ) & 0xFF);
		output.put8Bit(2, L & 0xFF);
		output.put8Bit(3, ( L >> 8 ) & 0xFF);
		output.put8Bit(4, U & 0xFF);
		output.put8Bit(5, ( U >> 8 ) & 0xFF);
	}
	
	private final void DSPOp03()
	{
		int F = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int L = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int U = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int X = (F * matrixA[0][0] >> 15) + (L * matrixA[1][0] >> 15) + (U * matrixA[2][0] >> 15);
		int Y = (F * matrixA[0][1] >> 15) + (L * matrixA[1][1] >> 15) + (U * matrixA[2][1] >> 15);
		int Z = (F * matrixA[0][2] >> 15) + (L * matrixA[1][2] >> 15) + (U * matrixA[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, X & 0xFF);
		output.put8Bit(1, ( X >> 8 ) & 0xFF);
		output.put8Bit(2, Y & 0xFF);
		output.put8Bit(3, ( Y >> 8 ) & 0xFF);
		output.put8Bit(4, Z & 0xFF);
		output.put8Bit(5, ( Z >> 8 ) & 0xFF);
	}

	private final void DSPOp13()
	{		
		int F = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int L = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int U = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int X = (F * matrixB[0][0] >> 15) + (L * matrixB[1][0] >> 15) + (U * matrixB[2][0] >> 15);
		int Y = (F * matrixB[0][1] >> 15) + (L * matrixB[1][1] >> 15) + (U * matrixB[2][1] >> 15);
		int Z = (F * matrixB[0][2] >> 15) + (L * matrixB[1][2] >> 15) + (U * matrixB[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, X & 0xFF);
		output.put8Bit(1, ( X >> 8 ) & 0xFF);
		output.put8Bit(2, Y & 0xFF);
		output.put8Bit(3, ( Y >> 8 ) & 0xFF);
		output.put8Bit(4, Z & 0xFF);
		output.put8Bit(5, ( Z >> 8 ) & 0xFF);
	}
	
	private final void DSPOp23()
	{
		int F = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int L = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int U = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int X = (F * matrixC[0][0] >> 15) + (L * matrixC[1][0] >> 15) + (U * matrixC[2][0] >> 15);
		int Y = (F * matrixC[0][1] >> 15) + (L * matrixC[1][1] >> 15) + (U * matrixC[2][1] >> 15);
		int Z = (F * matrixC[0][2] >> 15) + (L * matrixC[1][2] >> 15) + (U * matrixC[2][2] >> 15);
	
		out_count = 6;
		output.put8Bit(0, X & 0xFF);
		output.put8Bit(1, ( X >> 8 ) & 0xFF);
		output.put8Bit(2, Y & 0xFF);
		output.put8Bit(3, ( Y >> 8 ) & 0xFF);
		output.put8Bit(4, Z & 0xFF);
		output.put8Bit(5, ( Z >> 8 ) & 0xFF);
	}
	
	private final void DSPOp0B()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int S = (X * matrixA[0][0] + Y * matrixA[0][1] + Z * matrixA[0][2]) >> 15;
	
		out_count = 2;
		output.put8Bit(0, S & 0xFF);
		output.put8Bit(1, ( S >> 8 ) & 0xFF);
	}

	private final void DSPOp1B()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int S = (X * matrixB[0][0] + Y * matrixB[0][1] + Z * matrixB[0][2]) >> 15;
	
		out_count = 2;
		output.put8Bit(0, S & 0xFF);
		output.put8Bit(1, ( S >> 8 ) & 0xFF);
	}
	
	private final void DSPOp2B()
	{
		int X = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Y = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Z = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
	
		int S = (X * matrixC[0][0] + Y * matrixC[0][1] + Z * matrixC[0][2]) >> 15;
	
		out_count = 2;
		output.put8Bit(0, S & 0xFF);
		output.put8Bit(1, ( S >> 8 ) & 0xFF);
	}
	
	private final void DSPOp14()
	{
		int[] result;
		
		int Zr = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		int Xr = parameters.get8Bit(2) | ( parameters.get8Bit(3) << 8);
		int Yr = parameters.get8Bit(4) | ( parameters.get8Bit(5) << 8);
		int U = parameters.get8Bit(6) | ( parameters.get8Bit(7) << 8);
		int F = parameters.get8Bit(8) | ( parameters.get8Bit(9) << 8);
		int L = parameters.get8Bit(10) | ( parameters.get8Bit(11) << 8);

		int CSec, ESec, CTan, CSin, C, E;

		result = DSP1_Inverse(DSP1_Cos(Xr), 0);
		CSec = result[0];
		ESec = result[1];

		// Rotation Around Z
		result = DSP1_NormalizeDouble(U * DSP1_Cos(Yr) - F * DSP1_Sin(Yr));
		C = result[0];
		E = result[1];

		E = ESec - E;

		result = DSP1_Normalize(C * CSec >> 15, E);
		C = result[0];
		E = result[1];

		int Zrr = Zr + DSP1_Truncate(C, E);

		// Rotation Around X
		int Xrr = Xr + (U * DSP1_Sin(Yr) >> 15) + (F * DSP1_Cos(Yr) >> 15);

		// Rotation Around Y
		result = DSP1_NormalizeDouble(U * DSP1_Cos(Yr) + F * DSP1_Sin(Yr));
		C = result[0];
		E = result[1];

		E = ESec - E;

		DSP1_Normalize(DSP1_Sin(Xr), E);
		CSin = result[0];
		E = result[1];

		CTan = CSec * CSin >> 15;

		DSP1_Normalize(-(C * CTan >> 15), E);
		C = result[0];
		E = result[1];

		int Yrr = Yr + DSP1_Truncate(C, E) + L;

		out_count = 6;
		output.put8Bit(0, Zrr & 0xFF);
		output.put8Bit(1, ( Zrr >> 8 ) &0xFF);
		output.put8Bit(2,  Xrr & 0xFF);
		output.put8Bit(3, ( Xrr >> 8 ) & 0xFF);
		output.put8Bit(4, Yrr & 0xFF);
		output.put8Bit(5, ( Yrr >> 8 ) & 0xFF);
	}
	
	private final void DSPOp2F()
	{
		//int Unknown = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
	
		int Size=0x100;
	
		out_count = 2;
		output.put8Bit(0, Size & 0xFF);
		output.put8Bit(1, ( Size >> 8 ) & 0xFF);
	}

	private final void DSPOp0F()
	{
		//int Ramsize = parameters.get8Bit(0) | ( parameters.get8Bit(1) << 8);
		
		int Pass = 0x0000;
		
		out_count = 2;
		output.put8Bit(0, Pass & 0xFF );
		output.put8Bit(1, ( Pass >> 8 ) & 0xFF);
		
	}
	
	private int[] DSP1_Inverse(int Coefficient, int Exponent)
	{
		int iCoefficient, iExponent;
		int[] result = new int[2];
		
		// Step One: Division by Zero
		if (Coefficient == 0x0000)
		{
			iCoefficient = 0x7fff;
			iExponent = 0x002f;
		}
		else
		{
			int Sign = 1;
	
			// Step Two: Remove Sign
			if (Coefficient < 0)
			{
				if (Coefficient < -32767)
				{
					Coefficient = -32767;
				}
				
				Coefficient = Math.abs(Coefficient);
				Sign = -1;
			}
	
			// Step Three: Normalize
			while (Coefficient < 0x4000)
			{
				Coefficient <<= 1;
				Exponent--;
			}
	
			// Step Four: Special Case
			if (Coefficient == 0x4000)
				if (Sign == 1) iCoefficient = 0x7fff;
				else  {
					iCoefficient = -0x4000;
					Exponent--;
				}
			else {
				// Step Five: Initial Guess
				int i = DSP1ROM[((Coefficient - 0x4000) >> 7) + 0x0065];
	
				// Step Six: Iterate "estimated" Newton's Method
				i = (i + (-i * (Coefficient * i >> 15) >> 15)) << 1;
				i = (i + (-i * (Coefficient * i >> 15) >> 15)) << 1;
	
				iCoefficient = i * Sign;
			}
	
			iExponent = 1 - Exponent;
		}
		
		result[0] = iCoefficient;
		result[1] = iExponent;
		
		return result;
	}
	
	private int DSP1_Sin(int Angle)
	{
		int S;
		if (Angle < 0) {
			if (Angle == -32768) return 0;
			return -DSP1_Sin(-Angle);
		}
		S = DSP1_SinTable[Angle >> 8] + (DSP1_MulTable[Angle & 0xff] * DSP1_SinTable[0x40 + (Angle >> 8)] >> 15);
		if (S > 32767) S = 32767;
		
		return S;
	}

	private int DSP1_Cos(int Angle)
	{
	  int S;
		if (Angle < 0) {
			if (Angle == -32768) return -32768;
			Angle = -Angle;
		}
		S = DSP1_SinTable[0x40 + (Angle >> 8)] - (DSP1_MulTable[Angle & 0xff] * DSP1_SinTable[Angle >> 8] >> 15);
		if (S < -32768) S = -32767;
		
		return S;
	}
	
	private int[] DSP1_NormalizeDouble( int Product )
	{
		int Coefficient;
		int Exponent;
		
		int n = Product & 0x7fff;
		int m = Product >> 15;
		int i = 0x4000;
		int e = 0;

		if (m < 0)
			while ( ( (m & i) > 0 ) && ( i > 0 ) )
			{
				i >>= 1;
				e++;
			}
		else
			while ( ! ( ( (m & i) > 0 ) && ( i > 0 ) ) )
			{
				i >>= 1;
				e++;
			}

		if (e > 0)
		{
			Coefficient = m * DSP1ROM[0x0021 + e] << 1;

			if (e < 15)
				Coefficient += n * DSP1ROM[0x0040 - e] >> 15;
			else
			{
				i = 0x4000;

				if (m < 0)
					while ( ( ( n & i) > 0 ) && ( i > 0 ) )
					{
						i >>= 1;
						e++;
					}
				else
					while ( ! ( ( ( n & i ) > 0 ) && ( i > 0 ) ) )
					{
						i >>= 1;
						e++;
					}

				if (e > 15)
					Coefficient = n * DSP1ROM[0x0012 + e] << 1;
				else
					Coefficient += n;
			}
		}
		else
			Coefficient = m;

		Exponent = e;
		
		int[] pair = {Coefficient, Exponent}; 
		
		return pair;
	}
	
	private int DSP1_Truncate(int C, int E)
	{
		if (E > 0) {
			if (C > 0) return 32767; else if (C < 0) return -32767;
		} else {
			if (E < 0) return C * DSP1ROM[0x0031 + E] >> 15;
		}
		return C;
	}
	
	private int[] DSP1_Normalize(int m, int Exponent)
	{
		int[] result = new int[2];
		int Coefficient;
		int i = 0x4000;
		int e = 0;

		if (m < 0)
			while ( ( (m & i) > 0 ) && ( i > 0 ) ) {
				i >>= 1;
				e++;
			}
		else
			while ( ! ( ( (m & i) > 0 ) && ( i > 0 ) ) ) {
				i >>= 1;
				e++;
			}

		if (e > 0)
			Coefficient = m * DSP1ROM[0x21 + e] << 1;
		else
			Coefficient = m;

		Exponent -= e;
		
		result[0] = Coefficient;
		result[1] = Exponent;
		
		return result;
	}
	
	private final int DSP1_ShiftR( int C, int E )
	{
	  return C * DSP1ROM[0x0031 + E] >> 15;
	}
	
	void PreSaveDSP1()
	{
		//TODO: PreSaveDSP1
	}

	void PostLoadDSP1()
	{
		// TODO: PostLoadDSP1
	}
}
