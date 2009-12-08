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

class SuperFX
{
	// FxChip registers
	private int[] avReg = new int[16];		// 16 Generic registers
	private int vColorReg;		// Internal color register
	private int vPlotOptionReg;		// Plot option register
	private int vStatusReg;		// Status register
	private int vPrgBankReg;		// Program bank index register
	private int vRomBankReg;		// Rom bank index register
	private int vRamBankReg;		// Ram bank index register
	private int vCacheBaseReg;	// Cache base address register
	private int vCacheFlags;		// Saying what parts of the cache was written to
	private int vLastRamAdr;		// Last RAM address accessed
	private int pvDreg;			// Pointer to current destination register
	private int pvSreg;			// Pointer to current source register
	private int vRomBuffer;		// Current byte read by R14
	private int vPipe;			// Instruction set pipe
	private int vPipeAdr;		// The address of where the pipe was read from

	// status register optimization stuff
	private int vSign;			// v & 0x8000
	private int vZero;			// v == 0
	private int vCarry;			// a value of 1 or 0
	private int vOverflow;		// (v >= 0x8000 || v < -0x8000)

	// Other emulator variables

	private int vErrorCode;
	private int vIllegalAddress;

	private int bBreakPoint;
	private int vBreakPoint;
	private int vStepPoint;

	ByteArrayOffset pvRegisters;	// 768 bytes located in the memory at address 0x3000
	int nRamBanks;	// Number of 64kb-banks in FxRam (Don't confuse it with SNES-Ram!!!)
	ByteArrayOffset pvRam;		// Pointer to FxRam
	int nRomBanks;	// Number of 32kb-banks in Cart-ROM
	ByteArrayOffset pvRom;		// Pointer to Cart-ROM

	private int vMode;		// Color depth/mode
	private int vPrevMode;	// Previous depth
	private ByteArrayOffset pvScreenBase;
	private ByteArrayOffset[] apvScreen = new ByteArrayOffset[32];		// Pointer to each of the 32 screen colums
	private int[] x = new int[32];
	private int vScreenHeight;		// 128, 160, 192 or 256 (could be overriden by cmode)
	private int vScreenRealHeight;	// 128, 160, 192 or 256
	private int vPrevScreenHeight;
	private int vScreenSize;

	private ByteArrayOffset pvRamBank;		// Pointer to current RAM-bank
	private ByteArrayOffset pvRomBank;		// Pointer to current ROM-bank
	private ByteArrayOffset pvPrgBank;		// Pointer to current program ROM-bank

	private ByteArrayOffset[] apvRamBank = new ByteArrayOffset[FX_RAM_BANKS];// Ram bank table (max 256kb)
	private ByteArrayOffset[] apvRomBank = new ByteArrayOffset[256];	// Rom bank table

	private int bCacheActive;
	private ByteArrayOffset pvCache;		// Pointer to the GSU cache
	private int[] avCacheBackup = new int[512];	// Backup of ROM when the cache has replaced it
	private int vCounter;
	private int vInstCount;
	private boolean vSCBRDirty;		// if SCBR is written, our cached screen pointers need updating
	
	private static int avHeight[] = { 128, 160, 192, 256 };
	private static int avMult[] = { 16, 32, 32, 64 };
	
	private static final int FX_RAM_BANKS = 4;
	
	private static final boolean CHECK_LIMITS = false;
	private static final boolean FX_DO_ROMBUFFER = true;

	// GSU registers
	static final int GSU_R0 = 0x000;
	static final int GSU_R1 = 0x002;
	static final int GSU_R2 = 0x004;
	static final int GSU_R3 = 0x006;
	static final int GSU_R4 = 0x008;
	static final int GSU_R5 = 0x00a;
	static final int GSU_R6 = 0x00c;
	static final int GSU_R7 = 0x00e;
	static final int GSU_R8 = 0x010;
	static final int GSU_R9 = 0x012;
	static final int GSU_R10 = 0x014;
	static final int GSU_R11 = 0x016;
	static final int GSU_R12 = 0x018;
	static final int GSU_R13 = 0x01a;
	static final int GSU_R14 = 0x01c;
	static final int GSU_R15 = 0x01e;
	static final int GSU_SFR = 0x030;
	static final int GSU_BRAMR = 0x033;
	static final int GSU_PBR = 0x034;
	static final int GSU_ROMBR = 0x036;
	static final int GSU_CFGR = 0x037;
	static final int GSU_SCBR = 0x038;
	static final int GSU_CLSR = 0x039;
	static final int GSU_SCMR = 0x03a;
	static final int GSU_VCR = 0x03b;
	static final int GSU_RAMBR = 0x03c;
	static final int GSU_CBR = 0x03e;
	static final int GSU_CACHERAM = 0x100;

	// SFR flags
	static final int FLG_Z = (1<<1);
	static final int FLG_CY = (1<<2);
	static final int FLG_S = (1<<3);
	static final int FLG_OV = (1<<4);
	static final int FLG_G = (1<<5);
	static final int FLG_R = (1<<6);
	static final int FLG_ALT1 = (1<<8);
	static final int FLG_ALT2 = (1<<9);
	static final int FLG_IL = (1<<10);
	static final int FLG_IH = (1<<11);
	static final int FLG_B = (1<<12);
	static final int FLG_IRQ = (1<<15);

	private static final int FX_FLAG_ADDRESS_CHECKING = 0x01;
	private static final int FX_FLAG_ROM_BUFFER = 0x02;

	private static final int FX_BREAKPOINT = -1;
	private static final int FX_ERROR_ILLEGAL_ADDRESS = -2;
	
	// Depricated 
	class SFxInit
	{	
		private int					vFlags;
		private ByteArrayOffset	pvRegisters;	// 768 bytes located in the memory at address 0x3000 
		private int					nRamBanks;	  // Number of 64kb-banks in GSU-RAM/BackupRAM (banks 0x70-0x73)
		private ByteArrayOffset	pvRam;		  // Pointer to GSU-RAM
		private int					nRomBanks;	  // Number of 32kb-banks in Cart-ROM
		private ByteArrayOffset	pvRom;		  // Pointer to Cart-ROM
		private int					speedPerLine;
		private boolean				oneLineDone;
	}
	private SFxInit FxInit = new SFxInit();
	
	void ResetSuperFX ()
	{
		Globals globals = Globals.globals;
		SuperFX superfx = globals.superfx;
		
		superfx.vFlags = 0;
		superfx.speedPerLine = (int) (0.417 * 10.5e6 * ((1.0 / (double) globals.memory.ROMFramesPerSecond) / ((double) (globals.timings.V_Max))));
		superfx.oneLineDone = false;

		superfx.FxReset();
	}
	
	
	int	vFlags;
	int	speedPerLine;
	boolean	oneLineDone;
	
	private void fx_stop()
	{
		vStatusReg &= ~FLG_G;
		
		vCounter = 0;
		vInstCount = vCounter;

		// Check if we need to generate an IRQ
		if( (pvRegisters.get8Bit(GSU_CFGR) & 0x80) == 0)
			vStatusReg |= FLG_IRQ;

		vPlotOptionReg = 0;
		vPipe = 1;
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_nop()
	{ 
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_cache()
	{
		int c = avReg[15] & 0xfff0;
		if( vCacheBaseReg != c || bCacheActive == 0)
		{
			fx_flushCache();
			vCacheBaseReg = c;
			bCacheActive = 1;
		}
		avReg[15]++;
		CLRFLAGS();
	}
	
	private void fx_lsr()
	{
		vCarry = SREG() & 1;
		int v = USEX16( SREG() ) >> 1;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}

	private void fx_rol()
	{
		int v = USEX16((SREG() << 1) + vCarry);
		vCarry = (SREG() >> 15) & 1;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_bra()
	{
		int v = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15] += SEX8(v);
	}
	
	private void BRA_COND(boolean cond)
	{
		int v = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		
		if( cond )
		{
			avReg[15] += SEX8(v); 
		}
		else
		{
			avReg[15]++;
		}
	}
	
	// 06 - blt - branch on less than
	private void fx_blt()
	{
		BRA_COND( ( (vSign & 0x8000) != 0) != (vOverflow >= 0x8000 || vOverflow < -0x8000) );
	}

	// 07 - bge - branch on greater or equals
	private void fx_bge()
	{
		BRA_COND( ( ( vSign & 0x8000 ) != 0) == (vOverflow >= 0x8000 || vOverflow < -0x8000) );
	}

	// 08 - bne - branch on not equal
	private void fx_bne()
	{
		BRA_COND( USEX16(vZero) != 0 );
	}

	// 09 - beq - branch on equal
	private void fx_beq()
	{
		BRA_COND( USEX16(vZero) == 0 );
	}

	// 0a - bpl - branch on plus
	private void fx_bpl()
	{
		BRA_COND( (vSign & 0x8000) == 0 );
	}

	// 0b - bmi - branch on minus
	private void fx_bmi()
	{
		BRA_COND( ( vSign & 0x8000 ) != 0 );
	}

	// 0c - bcc - branch on carry clear
	private void fx_bcc()
	{
		BRA_COND( ( vCarry & 1 ) == 0 );
	}

	// 0d - bcs - branch on carry set
	private void fx_bcs()
	{
		BRA_COND( ( vCarry & 1 ) != 0 );
	}

	// 0e - bvc - branch on overflow clear
	private void fx_bvc()
	{
		BRA_COND( ! (vOverflow >= 0x8000 || vOverflow < -0x8000) );
	}

	// 0f - bvs - branch on overflow set
	private void fx_bvs()
	{
		BRA_COND( vOverflow >= 0x8000 || vOverflow < -0x8000 );
	}

	private void FX_TO_R14(int reg)
	{
		if( ( vStatusReg & FLG_B) != 0 )
		{
			avReg[reg] = SREG();
			CLRFLAGS();
			READR14();
		} else {
			pvDreg = reg;
			avReg[15]++;
		}
	}
	
	private void FX_TO_R15(int reg)
	{
		if( ( vStatusReg & FLG_B) != 0 )
		{
			avReg[reg] = SREG();
			CLRFLAGS();
		} else {
			pvDreg = reg;
			avReg[15]++;
		}
	}
	
	private void FX_TO(int reg)
	{
		if( ( vStatusReg & FLG_B) != 0)
		{
			avReg[reg] = SREG();
			CLRFLAGS();
		} else {
			pvDreg = reg;
		}
		avReg[15]++;
	}
	
	private void fx_to_r0() { FX_TO(0); }
	private void fx_to_r1() { FX_TO(1); }
	private void fx_to_r2() { FX_TO(2); }
	private void fx_to_r3() { FX_TO(3); }
	private void fx_to_r4() { FX_TO(4); }
	private void fx_to_r5() { FX_TO(5); }
	private void fx_to_r6() { FX_TO(6); }
	private void fx_to_r7() { FX_TO(7); }
	private void fx_to_r8() { FX_TO(8); }
	private void fx_to_r9() { FX_TO(9); }
	private void fx_to_r10() { FX_TO(10); }
	private void fx_to_r11() { FX_TO(11); }
	private void fx_to_r12() { FX_TO(12); }
	private void fx_to_r13() { FX_TO(13); }
	private void fx_to_r14() { FX_TO_R14(14); }
	private void fx_to_r15() { FX_TO_R15(15); }
	

	private void FX_STW( int reg )
	{
		vLastRamAdr = avReg[reg];
		
		pvRamBank.put8Bit(USEX16(avReg[reg]), SREG() & 0xFF);
		pvRamBank.put8Bit(USEX16(avReg[reg] ^ 1), (SREG() >> 8) & 0xFF );
					   
		CLRFLAGS();
		avReg[15]++;
	}

	private void fx_stw_r0() { FX_STW(0); }
	private void fx_stw_r1() { FX_STW(1); }
	private void fx_stw_r2() { FX_STW(2); }
	private void fx_stw_r3() { FX_STW(3); }
	private void fx_stw_r4() { FX_STW(4); }
	private void fx_stw_r5() { FX_STW(5); }
	private void fx_stw_r6() { FX_STW(6); }
	private void fx_stw_r7() { FX_STW(7); }
	private void fx_stw_r8() { FX_STW(8); }
	private void fx_stw_r9() { FX_STW(9); }
	private void fx_stw_r10() { FX_STW(10); }
	private void fx_stw_r11() { FX_STW(11); }
	
	private void  FX_WITH(int reg)
	{
		vStatusReg |= FLG_B;
		pvSreg = reg;
		pvDreg = reg;
		avReg[15]++;
	}
	
	private void fx_with_r0() { FX_WITH(0); }
	private void fx_with_r1() { FX_WITH(1); }
	private void fx_with_r2() { FX_WITH(2); }
	private void fx_with_r3() { FX_WITH(3); }
	private void fx_with_r4() { FX_WITH(4); }
	private void fx_with_r5() { FX_WITH(5); }
	private void fx_with_r6() { FX_WITH(6); }
	private void fx_with_r7() { FX_WITH(7); }
	private void fx_with_r8() { FX_WITH(8); }
	private void fx_with_r9() { FX_WITH(9); }
	private void fx_with_r10() { FX_WITH(10); }
	private void fx_with_r11() { FX_WITH(11); }
	private void fx_with_r12() { FX_WITH(12); }
	private void fx_with_r13() { FX_WITH(13); }
	private void fx_with_r14() { FX_WITH(14); }
	private void fx_with_r15() { FX_WITH(15); }
	
	private void FX_STB(int reg)
	{
		vLastRamAdr = avReg[reg];
		pvRamBank.put8Bit(USEX16(avReg[reg]), SREG() & 0xFF);
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_stb_r0() { FX_STB(0); }
	private void fx_stb_r1() { FX_STB(1); }
	private void fx_stb_r2() { FX_STB(2); }
	private void fx_stb_r3() { FX_STB(3); }
	private void fx_stb_r4() { FX_STB(4); }
	private void fx_stb_r5() { FX_STB(5); }
	private void fx_stb_r6() { FX_STB(6); }
	private void fx_stb_r7() { FX_STB(7); }
	private void fx_stb_r8() { FX_STB(8); }
	private void fx_stb_r9() { FX_STB(9); }
	private void fx_stb_r10() { FX_STB(10); }
	private void fx_stb_r11() { FX_STB(11); }
	
	private void fx_loop()
	{
		--avReg[12];
		vSign = avReg[12];
		vZero = avReg[12];
		
		if( (avReg[12] & 0xFFFF ) != 0 )
			avReg[15] = avReg[13];
		else
			avReg[15]++;

		CLRFLAGS();
	}

	private void fx_alt1()
	{
		vStatusReg |= FLG_ALT1;
		vStatusReg &= ~FLG_B;
		avReg[15]++;
	}

	private void fx_alt2()
	{
		vStatusReg |= FLG_ALT2;
		vStatusReg &= ~FLG_B;
		avReg[15]++;
	}

	private void fx_alt3()
	{
		vStatusReg |= FLG_ALT1;
		vStatusReg |= FLG_ALT2;
		vStatusReg &= ~FLG_B;
		avReg[15]++;
	}
	
	
	private void FX_LDW(int reg)
	{
		vLastRamAdr = avReg[reg];
		int v = pvRamBank.get8Bit( USEX16( avReg[reg] ) );
		v |= ( pvRamBank.get8Bit( USEX16( avReg[reg] ^ 1) ) ) << 8;
		avReg[15]++;
		DREG(v);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_ldw_r0() { FX_LDW(0); }
	private void fx_ldw_r1() { FX_LDW(1); }
	private void fx_ldw_r2() { FX_LDW(2); }
	private void fx_ldw_r3() { FX_LDW(3); }
	private void fx_ldw_r4() { FX_LDW(4); }
	private void fx_ldw_r5() { FX_LDW(5); }
	private void fx_ldw_r6() { FX_LDW(6); }
	private void fx_ldw_r7() { FX_LDW(7); }
	private void fx_ldw_r8() { FX_LDW(8); }
	private void fx_ldw_r9() { FX_LDW(9); }
	private void fx_ldw_r10() { FX_LDW(10); }
	private void fx_ldw_r11() { FX_LDW(11); }
	
	private void FX_LDB(int reg)
	{
		vLastRamAdr = avReg[reg];
		int v = pvRamBank.get8Bit( USEX16( avReg[reg]) );
		avReg[15]++;
		DREG(v);
		TESTR14();
		CLRFLAGS();
	}
	private void fx_ldb_r0() { FX_LDB(0); }
	private void fx_ldb_r1() { FX_LDB(1); }
	private void fx_ldb_r2() { FX_LDB(2); }
	private void fx_ldb_r3() { FX_LDB(3); }
	private void fx_ldb_r4() { FX_LDB(4); }
	private void fx_ldb_r5() { FX_LDB(5); }
	private void fx_ldb_r6() { FX_LDB(6); }
	private void fx_ldb_r7() { FX_LDB(7); }
	private void fx_ldb_r8() { FX_LDB(8); }
	private void fx_ldb_r9() { FX_LDB(9); }
	private void fx_ldb_r10() { FX_LDB(10); }
	private void fx_ldb_r11() { FX_LDB(11); }
	
	private void fx_plot_2bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v,c;

		avReg[15]++;
		CLRFLAGS();
		avReg[1]++;

		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}

		if( (vPlotOptionReg & 0x02 ) != 0 )
			c = ( ( (x ^ y) & 1 ) == 1 ? vColorReg >> 4 : vColorReg ) & 0xFF;
		else
			c = vColorReg & 0xFF;

		if( (vPlotOptionReg & 0x01) == 0 && (c & 0xf) == 0)
			return;
		
		// NAC: I think this is right
		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		
		v = 128 >> (x & 7);

		if( (c & 0x01 ) != 0 )
		{
			a.buffer[a.getOffset()] |= v;
		} else {
			a.buffer[a.getOffset()] &= ~v;
		}
		
		if( ( c & 0x02 ) != 0 )
		{
			a.buffer[a.getOffset() + 1] |= v;
		} else {
			a.buffer[a.getOffset() + 1] &= ~v;
		}
	}
	
	private void fx_rpix_2bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v;

		avReg[15]++;
		CLRFLAGS();
		
		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}

		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		
		v = 128 >> (x&7);

		int dreg_temp = 0;
		dreg_temp |= ( ( a.buffer[a.getOffset()] & v ) & 1 ) << 0;
		dreg_temp |= ( ( a.buffer[a.getOffset() + 1] & v ) & 1 ) << 1;
		
		DREG(dreg_temp);
		TESTR14();
	}

	private void fx_plot_4bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v,c;

		avReg[15]++;
		CLRFLAGS();
		avReg[1]++;

		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}

		if( ( vPlotOptionReg & 0x02 ) != 0 )
		{
			c = ( ( ( x ^ y ) & 1 ) == 1 ? (vColorReg >>4 ) : vColorReg )  & 0xFF;
		} else {
			c = vColorReg & 0xFF;
		}

		if( (vPlotOptionReg & 0x01) == 0 && (c & 0xf) == 0) return;

		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		v = 128 >> (x & 7);

		if( ( c & 0x01 ) != 0 )
			a.buffer[a.getOffset()] |= v;
		else
			a.buffer[a.getOffset()] &= ~v;
		
		if( ( c & 0x02 ) != 0 )
			a.buffer[a.getOffset() + 1] |= v;
		else
			a.buffer[a.getOffset() + 1] &= ~v;
		
		if( ( c & 0x04 ) != 0 )
			a.buffer[a.getOffset() + 0x10] |= v;
		else
			a.buffer[a.getOffset() + 0x10] &= ~v;
		
		if( ( c & 0x08 ) != 0 )
			a.buffer[a.getOffset() + 0x11] |= v;
		else
			a.buffer[a.getOffset() + 0x11] &= ~v;
	}
	
	private void fx_rpix_4bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v;

		avReg[15]++;
		CLRFLAGS();

		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}

		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		v = 128 >> (x&7);

		int dreg_temp = 0;
		dreg_temp |= (((a.buffer[a.getOffset()] & v) & 1)) << 0;
		dreg_temp |= (((a.buffer[a.getOffset() + 0x01] & v) & 1)) << 1;
		dreg_temp |= (((a.buffer[a.getOffset() + 0x10] & v) & 1)) << 2;
		dreg_temp |= (((a.buffer[a.getOffset() + 0x11] & v) & 1)) << 3;
		DREG(dreg_temp);
		
		TESTR14();
	}
	
	private void fx_plot_8bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v,c;

		avReg[15]++;
		CLRFLAGS();
		avReg[1]++;

		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}
		
		c = vColorReg & 0xFF;
		
		if( (vPlotOptionReg & 0x10) == 0 )
		{
			if( (vPlotOptionReg & 0x01) == 0 && (c & 0xf) == 0) return;
		}
		
		else
		{
			if( (vPlotOptionReg & 0x01) == 0 && c == 0) return;
		}

		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		v = 128 >> ( x & 7);

		if( ( c & 0x01 ) != 0 )
			a.buffer[a.getOffset()] |= v;
		else
			a.buffer[a.getOffset()] &= ~v;
		
		if( ( c & 0x02 ) != 0)
			a.buffer[a.getOffset() + 0x01] |= v;
		else
			a.buffer[a.getOffset() + 0x01] &= ~v;
		
		if( ( c & 0x04 ) != 0)
			a.buffer[a.getOffset() + 0x10] |= v;
		else
			a.buffer[a.getOffset() + 0x10] &= ~v;
		
		if( ( c & 0x08) != 0)
			a.buffer[a.getOffset() + 0x11] |= v;
		else
			a.buffer[a.getOffset() + 0x11] &= ~v;
		
		if( ( c & 0x10 ) != 0 )
			a.buffer[a.getOffset() + 0x20] |= v;
		else 
			a.buffer[a.getOffset() + 0x20] &= ~v;
		
		if( ( c & 0x20 ) != 0)
			a.buffer[a.getOffset() + 0x21] |= v;
		else
			a.buffer[a.getOffset() + 0x21] &= ~v;
		
		if( ( c & 0x40 ) != 0)
			a.buffer[a.getOffset() + 0x30] |= v;
		else
			a.buffer[a.getOffset() + 0x30] &= ~v;
		
		if( ( c & 0x80 ) != 0)
			a.buffer[a.getOffset() + 0x31] |= v;
		else 
			a.buffer[a.getOffset() + 0x31] &= ~v;
	}
	
	private void fx_rpix_8bit()
	{
		int x = USEX8(avReg[1]);
		int y = USEX8(avReg[2]);
		ByteArrayOffset a;
		int v;

		avReg[15]++;
		CLRFLAGS();

		if (SuperFX.CHECK_LIMITS)
		{
			if(y >= vScreenHeight) return;
		}
		
		a = apvScreen[y >> 3].getOffsetBuffer( this.x[x >> 3] + ((y & 7) << 1) );
		v = 128 >> (x&7);

		int dreg_temp = 0;
		dreg_temp |= ((a.buffer[a.getOffset()] & v) & 1 ) << 0;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x01] & v) & 1 ) << 1;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x10] & v) & 1 ) << 2;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x11] & v) & 1 ) << 3;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x20] & v) & 1 ) << 4;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x21] & v) & 1 ) << 5;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x30] & v) & 1 ) << 6;
		dreg_temp |= ((a.buffer[a.getOffset() + 0x31] & v) & 1 ) << 7;
		DREG(dreg_temp);
		
		vZero = dreg_temp;
		TESTR14();
	}
	
	private void fx_plot_obj()
	{
		// ERROR fx_plot_obj called
	}
	
	private void fx_rpix_obj()
	{
	   // ERROR fx_rpix_obj called
	}
	
	private void fx_swap()
	{
		int c = SREG() & 0xFF;
		int d = (SREG() >> 8)  & 0xFF;
		int v = ( c << 8 ) | d;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_color()
	{
		int c = SREG() & 0xFF; 
		if( ( vPlotOptionReg & 0x04 ) != 0 )
			c = (c&0xf0) | (c>>4);
		
		if( ( vPlotOptionReg & 0x08 ) != 0 )
		{
			vColorReg &= 0xf0;
			vColorReg |= c & 0x0f;
		}
		else
			vColorReg = USEX8(c);
		
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_cmode()
	{
		vPlotOptionReg = SREG();

		if( ( vPlotOptionReg & 0x10 ) != 0 )
		{
			// OBJ Mode (for drawing into sprites)
			vScreenHeight = 256;
		}
		else
			vScreenHeight = vScreenRealHeight;

		fx_computeScreenPointers();
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_not()
	{
		int v = ~SREG();
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void FX_ADD(int reg)
	{
		int s = SUSEX16(SREG()) + SUSEX16(avReg[reg]);
		vCarry = s >= 0x10000 ? 1 : 0;
		vOverflow = ~(SREG() ^ avReg[reg]) & (avReg[reg] ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_add_r0() { FX_ADD(0); }
	private void fx_add_r1() { FX_ADD(1); }
	private void fx_add_r2() { FX_ADD(2); }
	private void fx_add_r3() { FX_ADD(3); }
	private void fx_add_r4() { FX_ADD(4); }
	private void fx_add_r5() { FX_ADD(5); }
	private void fx_add_r6() { FX_ADD(6); }
	private void fx_add_r7() { FX_ADD(7); }
	private void fx_add_r8() { FX_ADD(8); }
	private void fx_add_r9() { FX_ADD(9); }
	private void fx_add_r10() { FX_ADD(10); }
	private void fx_add_r11() { FX_ADD(11); }
	private void fx_add_r12() { FX_ADD(12); }
	private void fx_add_r13() { FX_ADD(13); }
	private void fx_add_r14() { FX_ADD(14); }
	private void fx_add_r15() { FX_ADD(15); }
	
	private void FX_ADC(int reg)
	{
		int s = SUSEX16(SREG()) + SUSEX16(avReg[reg]) + SEX16(vCarry);
		vCarry = s >= 0x10000 ? 1 : 0;
		vOverflow = ~(SREG() ^ avReg[reg]) & (avReg[reg] ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_adc_r0() { FX_ADC(0); }
	private void fx_adc_r1() { FX_ADC(1); }
	private void fx_adc_r2() { FX_ADC(2); }
	private void fx_adc_r3() { FX_ADC(3); }
	private void fx_adc_r4() { FX_ADC(4); }
	private void fx_adc_r5() { FX_ADC(5); }
	private void fx_adc_r6() { FX_ADC(6); }
	private void fx_adc_r7() { FX_ADC(7); }
	private void fx_adc_r8() { FX_ADC(8); }
	private void fx_adc_r9() { FX_ADC(9); }
	private void fx_adc_r10() { FX_ADC(10); }
	private void fx_adc_r11() { FX_ADC(11); }
	private void fx_adc_r12() { FX_ADC(12); }
	private void fx_adc_r13() { FX_ADC(13); }
	private void fx_adc_r14() { FX_ADC(14); }
	private void fx_adc_r15() { FX_ADC(15); }

	private void FX_ADD_I( int imm)
	{
		int s = SUSEX16(SREG()) + imm;
		vCarry = s >= 0x10000 ? 1 : 0;
		vOverflow = ~(SREG() ^ imm) & (imm ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_add_i0() { FX_ADD_I(0); }
	private void fx_add_i1() { FX_ADD_I(1); }
	private void fx_add_i2() { FX_ADD_I(2); }
	private void fx_add_i3() { FX_ADD_I(3); }
	private void fx_add_i4() { FX_ADD_I(4); }
	private void fx_add_i5() { FX_ADD_I(5); }
	private void fx_add_i6() { FX_ADD_I(6); }
	private void fx_add_i7() { FX_ADD_I(7); }
	private void fx_add_i8() { FX_ADD_I(8); }
	private void fx_add_i9() { FX_ADD_I(9); }
	private void fx_add_i10() { FX_ADD_I(10); }
	private void fx_add_i11() { FX_ADD_I(11); }
	private void fx_add_i12() { FX_ADD_I(12); }
	private void fx_add_i13() { FX_ADD_I(13); }
	private void fx_add_i14() { FX_ADD_I(14); }
	private void fx_add_i15() { FX_ADD_I(15); }

	private void FX_ADC_I( int imm)
	{
		int s = SUSEX16(SREG()) + imm + SUSEX16(vCarry);
		vCarry = s >= 0x10000 ? 1 : 0;
		vOverflow = ~(SREG() ^ imm) & (imm ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
		
	private void fx_adc_i0() { FX_ADC_I(0); }
	private void fx_adc_i1() { FX_ADC_I(1); }
	private void fx_adc_i2() { FX_ADC_I(2); }
	private void fx_adc_i3() { FX_ADC_I(3); }
	private void fx_adc_i4() { FX_ADC_I(4); }
	private void fx_adc_i5() { FX_ADC_I(5); }
	private void fx_adc_i6() { FX_ADC_I(6); }
	private void fx_adc_i7() { FX_ADC_I(7); }
	private void fx_adc_i8() { FX_ADC_I(8); }
	private void fx_adc_i9() { FX_ADC_I(9); }
	private void fx_adc_i10() { FX_ADC_I(10); }
	private void fx_adc_i11() { FX_ADC_I(11); }
	private void fx_adc_i12() { FX_ADC_I(12); }
	private void fx_adc_i13() { FX_ADC_I(13); }
	private void fx_adc_i14() { FX_ADC_I(14); }
	private void fx_adc_i15() { FX_ADC_I(15); }
	
	private void FX_SUB( int reg )
	{
		int s = SUSEX16(SREG()) - SUSEX16(avReg[reg]);
		vCarry = s >= 0 ? 1 : 0;
		vOverflow = (SREG() ^ avReg[reg]) & (SREG() ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	private void fx_sub_r0() { FX_SUB(0); }
	private void fx_sub_r1() { FX_SUB(1); }
	private void fx_sub_r2() { FX_SUB(2); }
	private void fx_sub_r3() { FX_SUB(3); }
	private void fx_sub_r4() { FX_SUB(4); }
	private void fx_sub_r5() { FX_SUB(5); }
	private void fx_sub_r6() { FX_SUB(6); }
	private void fx_sub_r7() { FX_SUB(7); }
	private void fx_sub_r8() { FX_SUB(8); }
	private void fx_sub_r9() { FX_SUB(9); }
	private void fx_sub_r10() { FX_SUB(10); }
	private void fx_sub_r11() { FX_SUB(11); }
	private void fx_sub_r12() { FX_SUB(12); }
	private void fx_sub_r13() { FX_SUB(13); }
	private void fx_sub_r14() { FX_SUB(14); }
	private void fx_sub_r15() { FX_SUB(15); }
	
	private void FX_SBC( int reg )
	{
		int s = SUSEX16(SREG()) - SUSEX16(avReg[reg]) - (SUSEX16(vCarry^1));
		vCarry = s >= 0 ? 1 : 0;
		vOverflow = (SREG() ^ avReg[reg]) & (SREG() ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_sbc_r0() { FX_SBC(0); }
	private void fx_sbc_r1() { FX_SBC(1); }
	private void fx_sbc_r2() { FX_SBC(2); }
	private void fx_sbc_r3() { FX_SBC(3); }
	private void fx_sbc_r4() { FX_SBC(4); }
	private void fx_sbc_r5() { FX_SBC(5); }
	private void fx_sbc_r6() { FX_SBC(6); }
	private void fx_sbc_r7() { FX_SBC(7); }
	private void fx_sbc_r8() { FX_SBC(8); }
	private void fx_sbc_r9() { FX_SBC(9); }
	private void fx_sbc_r10() { FX_SBC(10); }
	private void fx_sbc_r11() { FX_SBC(11); }
	private void fx_sbc_r12() { FX_SBC(12); }
	private void fx_sbc_r13() { FX_SBC(13); }
	private void fx_sbc_r14() { FX_SBC(14); }
	private void fx_sbc_r15() { FX_SBC(15); }
	
	
	private void FX_SUB_I( int imm )
	{
		int s = SUSEX16(SREG()) - imm;
		vCarry = s >= 0 ? 1 : 0;
		vOverflow = (SREG() ^ imm) & (SREG() ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		DREG(s);
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_sub_i0() { FX_SUB_I(0); }
	private void fx_sub_i1() { FX_SUB_I(1); }
	private void fx_sub_i2() { FX_SUB_I(2); }
	private void fx_sub_i3() { FX_SUB_I(3); }
	private void fx_sub_i4() { FX_SUB_I(4); }
	private void fx_sub_i5() { FX_SUB_I(5); }
	private void fx_sub_i6() { FX_SUB_I(6); }
	private void fx_sub_i7() { FX_SUB_I(7); }
	private void fx_sub_i8() { FX_SUB_I(8); }
	private void fx_sub_i9() { FX_SUB_I(9); }
	private void fx_sub_i10() { FX_SUB_I(10); }
	private void fx_sub_i11() { FX_SUB_I(11); }
	private void fx_sub_i12() { FX_SUB_I(12); }
	private void fx_sub_i13() { FX_SUB_I(13); }
	private void fx_sub_i14() { FX_SUB_I(14); }
	private void fx_sub_i15() { FX_SUB_I(15); }

	private void  FX_CMP( int reg )
	{
		int s = SUSEX16(SREG()) - SUSEX16(avReg[reg]);
		vCarry = s >= 0 ? 1 : 0;
		vOverflow = (SREG() ^ avReg[reg]) & (SREG() ^ s) & 0x8000;
		vSign = s;
		vZero = s;
		avReg[15]++;
		CLRFLAGS();
	}
	
	private void fx_cmp_r0() { FX_CMP(0); }
	private void fx_cmp_r1() { FX_CMP(1); }
	private void fx_cmp_r2() { FX_CMP(2); }
	private void fx_cmp_r3() { FX_CMP(3); }
	private void fx_cmp_r4() { FX_CMP(4); }
	private void fx_cmp_r5() { FX_CMP(5); }
	private void fx_cmp_r6() { FX_CMP(6); }
	private void fx_cmp_r7() { FX_CMP(7); }
	private void fx_cmp_r8() { FX_CMP(8); }
	private void fx_cmp_r9() { FX_CMP(9); }
	private void fx_cmp_r10() { FX_CMP(10); }
	private void fx_cmp_r11() { FX_CMP(11); }
	private void fx_cmp_r12() { FX_CMP(12); }
	private void fx_cmp_r13() { FX_CMP(13); }
	private void fx_cmp_r14() { FX_CMP(14); }
	private void fx_cmp_r15() { FX_CMP(15); }
	
	private void fx_merge()
	{
		int v = (avReg[7] & 0xff00) | ( ( avReg[8] & 0xff00 ) >> 8 );
		avReg[15]++;
		DREG(v);
		vOverflow = (v & 0xc0c0) << 16;
		vZero = (v & 0xf0f0) != 0 ? 1 : 0;
		vSign = ( (v | ( v <<8 ) ) & 0x8000);
		vCarry = (v & 0xe0e0) != 0 ? 1 : 0;
		TESTR14();
		CLRFLAGS();
	}
	
	private void FX_AND( int reg )
	{
		int v = SREG() & avReg[reg];
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_and_r1() { FX_AND(1); }
	private void fx_and_r2() { FX_AND(2); }
	private void fx_and_r3() { FX_AND(3); }
	private void fx_and_r4() { FX_AND(4); }
	private void fx_and_r5() { FX_AND(5); }
	private void fx_and_r6() { FX_AND(6); }
	private void fx_and_r7() { FX_AND(7); }
	private void fx_and_r8() { FX_AND(8); }
	private void fx_and_r9() { FX_AND(9); }
	private void fx_and_r10() { FX_AND(10); }
	private void fx_and_r11() { FX_AND(11); }
	private void fx_and_r12() { FX_AND(12); }
	private void fx_and_r13() { FX_AND(13); }
	private void fx_and_r14() { FX_AND(14); }
	private void fx_and_r15() { FX_AND(15); }

	private void FX_BIC( int reg )
	{
		int v = SREG() & ~avReg[reg];
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_bic_r1() { FX_BIC(1); }
	private void fx_bic_r2() { FX_BIC(2); }
	private void fx_bic_r3() { FX_BIC(3); }
	private void fx_bic_r4() { FX_BIC(4); }
	private void fx_bic_r5() { FX_BIC(5); }
	private void fx_bic_r6() { FX_BIC(6); }
	private void fx_bic_r7() { FX_BIC(7); }
	private void fx_bic_r8() { FX_BIC(8); }
	private void fx_bic_r9() { FX_BIC(9); }
	private void fx_bic_r10() { FX_BIC(10); }
	private void fx_bic_r11() { FX_BIC(11); }
	private void fx_bic_r12() { FX_BIC(12); }
	private void fx_bic_r13() { FX_BIC(13); }
	private void fx_bic_r14() { FX_BIC(14); }
	private void fx_bic_r15() { FX_BIC(15); }

	private void FX_AND_I( int imm )
	{
		int v = SREG() & imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_and_i1() { FX_AND_I(1); }
	private void fx_and_i2() { FX_AND_I(2); }
	private void fx_and_i3() { FX_AND_I(3); }
	private void fx_and_i4() { FX_AND_I(4); }
	private void fx_and_i5() { FX_AND_I(5); }
	private void fx_and_i6() { FX_AND_I(6); }
	private void fx_and_i7() { FX_AND_I(7); }
	private void fx_and_i8() { FX_AND_I(8); }
	private void fx_and_i9() { FX_AND_I(9); }
	private void fx_and_i10() { FX_AND_I(10); }
	private void fx_and_i11() { FX_AND_I(11); }
	private void fx_and_i12() { FX_AND_I(12); }
	private void fx_and_i13() { FX_AND_I(13); }
	private void fx_and_i14() { FX_AND_I(14); }
	private void fx_and_i15() { FX_AND_I(15); }

	private void FX_BIC_I( int imm)
	{
		int v = SREG() & ~imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_bic_i1() { FX_BIC_I(1); }
	private void fx_bic_i2() { FX_BIC_I(2); }
	private void fx_bic_i3() { FX_BIC_I(3); }
	private void fx_bic_i4() { FX_BIC_I(4); }
	private void fx_bic_i5() { FX_BIC_I(5); }
	private void fx_bic_i6() { FX_BIC_I(6); }
	private void fx_bic_i7() { FX_BIC_I(7); }
	private void fx_bic_i8() { FX_BIC_I(8); }
	private void fx_bic_i9() { FX_BIC_I(9); }
	private void fx_bic_i10() { FX_BIC_I(10); }
	private void fx_bic_i11() { FX_BIC_I(11); }
	private void fx_bic_i12() { FX_BIC_I(12); }
	private void fx_bic_i13() { FX_BIC_I(13); }
	private void fx_bic_i14() { FX_BIC_I(14); }
	private void fx_bic_i15() { FX_BIC_I(15); }

	private void FX_MULT( int reg)
	{
		int v = (SEX8(SREG()) * SEX8(avReg[reg]));
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_mult_r0() { FX_MULT(0); }
	private void fx_mult_r1() { FX_MULT(1); }
	private void fx_mult_r2() { FX_MULT(2); }
	private void fx_mult_r3() { FX_MULT(3); }
	private void fx_mult_r4() { FX_MULT(4); }
	private void fx_mult_r5() { FX_MULT(5); }
	private void fx_mult_r6() { FX_MULT(6); }
	private void fx_mult_r7() { FX_MULT(7); }
	private void fx_mult_r8() { FX_MULT(8); }
	private void fx_mult_r9() { FX_MULT(9); }
	private void fx_mult_r10() { FX_MULT(10); }
	private void fx_mult_r11() { FX_MULT(11); }
	private void fx_mult_r12() { FX_MULT(12); }
	private void fx_mult_r13() { FX_MULT(13); }
	private void fx_mult_r14() { FX_MULT(14); }
	private void fx_mult_r15() { FX_MULT(15); }

	private void FX_UMULT( int reg )
	{
		int v = USEX8(SREG()) * USEX8(avReg[reg]);
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_umult_r0() { FX_UMULT(0); }
	private void fx_umult_r1() { FX_UMULT(1); }
	private void fx_umult_r2() { FX_UMULT(2); }
	private void fx_umult_r3() { FX_UMULT(3); }
	private void fx_umult_r4() { FX_UMULT(4); }
	private void fx_umult_r5() { FX_UMULT(5); }
	private void fx_umult_r6() { FX_UMULT(6); }
	private void fx_umult_r7() { FX_UMULT(7); }
	private void fx_umult_r8() { FX_UMULT(8); }
	private void fx_umult_r9() { FX_UMULT(9); }
	private void fx_umult_r10() { FX_UMULT(10); }
	private void fx_umult_r11() { FX_UMULT(11); }
	private void fx_umult_r12() { FX_UMULT(12); }
	private void fx_umult_r13() { FX_UMULT(13); }
	private void fx_umult_r14() { FX_UMULT(14); }
	private void fx_umult_r15() { FX_UMULT(15); }

	private void FX_MULT_I( int imm )
	{
		int v = SEX8(SREG()) * imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	private void fx_mult_i0() { FX_MULT_I(0); }
	private void fx_mult_i1() { FX_MULT_I(1); }
	private void fx_mult_i2() { FX_MULT_I(2); }
	private void fx_mult_i3() { FX_MULT_I(3); }
	private void fx_mult_i4() { FX_MULT_I(4); }
	private void fx_mult_i5() { FX_MULT_I(5); }
	private void fx_mult_i6() { FX_MULT_I(6); }
	private void fx_mult_i7() { FX_MULT_I(7); }
	private void fx_mult_i8() { FX_MULT_I(8); }
	private void fx_mult_i9() { FX_MULT_I(9); }
	private void fx_mult_i10() { FX_MULT_I(10); }
	private void fx_mult_i11() { FX_MULT_I(11); }
	private void fx_mult_i12() { FX_MULT_I(12); }
	private void fx_mult_i13() { FX_MULT_I(13); }
	private void fx_mult_i14() { FX_MULT_I(14); }
	private void fx_mult_i15() { FX_MULT_I(15); }

	private void FX_UMULT_I( int imm )
	{
		int v = USEX8(SREG()) * imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	private void fx_umult_i0() { FX_UMULT_I(0); }
	private void fx_umult_i1() { FX_UMULT_I(1); }
	private void fx_umult_i2() { FX_UMULT_I(2); }
	private void fx_umult_i3() { FX_UMULT_I(3); }
	private void fx_umult_i4() { FX_UMULT_I(4); }
	private void fx_umult_i5() { FX_UMULT_I(5); }
	private void fx_umult_i6() { FX_UMULT_I(6); }
	private void fx_umult_i7() { FX_UMULT_I(7); }
	private void fx_umult_i8() { FX_UMULT_I(8); }
	private void fx_umult_i9() { FX_UMULT_I(9); }
	private void fx_umult_i10() { FX_UMULT_I(10); }
	private void fx_umult_i11() { FX_UMULT_I(11); }
	private void fx_umult_i12() { FX_UMULT_I(12); }
	private void fx_umult_i13() { FX_UMULT_I(13); }
	private void fx_umult_i14() { FX_UMULT_I(14); }
	private void fx_umult_i15() { FX_UMULT_I(15); }

	private void fx_sbk()
	{
		pvRamBank.put8Bit(USEX16(vLastRamAdr), SREG() & 0xFF);
		pvRamBank.put8Bit(USEX16(vLastRamAdr ^ 1), (SREG() >> 8) & 0xFF);

		CLRFLAGS();
		avReg[15]++;
	}
	
	private void FX_LINK_I( int lkn )
	{
		avReg[11] = avReg[15] + lkn;
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_link_i1() { FX_LINK_I(1); }
	private void fx_link_i2() { FX_LINK_I(2); }
	private void fx_link_i3() { FX_LINK_I(3); }
	private void fx_link_i4() { FX_LINK_I(4); }

	private void fx_sex()
	{
		int v = SEX8( SREG() );
		avReg[15]++;
		DREG( v );
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}

	private void fx_asr()
	{
		int v;
		vCarry = SREG() & 1;
		v = SEX16( SREG() ) >> 1;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}

	private void fx_div2()
	{
		int v;
		int s = SEX16( SREG() );
		
		vCarry = s & 1;
		if(s == -1)
			v = 0;
		else
			v = s >> 1;
		
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}

	private void fx_ror()
	{
		int v = ( USEX16( SREG() ) >> 1 ) | ( vCarry << 15 );
		vCarry = SREG() & 1;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}

	private void FX_JMP(int reg)
	{
		avReg[15] = avReg[reg];
		CLRFLAGS();
	}
	
	private void fx_jmp_r8() { FX_JMP(8); }
	private void fx_jmp_r9() { FX_JMP(9); }
	private void fx_jmp_r10() { FX_JMP(10); }
	private void fx_jmp_r11() { FX_JMP(11); }
	private void fx_jmp_r12() { FX_JMP(12); }
	private void fx_jmp_r13() { FX_JMP(13); }

	private void FX_LJMP( int reg )
	{
		vPrgBankReg = avReg[reg] & 0x7f;
		pvPrgBank = apvRomBank[vPrgBankReg];
		avReg[15] = SREG();
		bCacheActive = 0;
		fx_cache();
		avReg[15]--;
	}
	private void fx_ljmp_r8() { FX_LJMP(8); }
	private void fx_ljmp_r9() { FX_LJMP(9); }
	private void fx_ljmp_r10() { FX_LJMP(10); }
	private void fx_ljmp_r11() { FX_LJMP(11); }
	private void fx_ljmp_r12() { FX_LJMP(12); }
	private void fx_ljmp_r13() { FX_LJMP(13); }

	// 9e - lob - set upper byte to zero (keep low byte)
	private void fx_lob()
	{
		int v = USEX8( SREG() );
		avReg[15]++;
		DREG(v);
		vSign = v << 8;
		vZero = v << 8;
		TESTR14();
		CLRFLAGS();
	}

	// 9f - fmult - 16 bit to 32 bit signed multiplication, upper 16 bits only
	private void fx_fmult()
	{
		int c = SEX16(SREG()) * SEX16( avReg[6] );
		int v = c >> 16;
		avReg[15]++;
		DREG( v );
		vSign = v;
		vZero = v;
		vCarry = (c >> 15) & 1;
		TESTR14();
		CLRFLAGS();
	}

	// 9f(ALT1) - lmult - 16 bit to 32 bit signed multiplication
	private void fx_lmult()
	{
		int c = SEX16(SREG()) * SEX16(avReg[6]);
		avReg[4] = c;
		int v = c >> 16;
		avReg[15]++;
		DREG( v );
		vSign = v;
		vZero = v;
		vCarry = (avReg[4] >> 15) & 1;	// should it be bit 15 of R4 instead?
		TESTR14();
		CLRFLAGS();
	}

	private void FX_IBT( int reg)
	{
		int v = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit(USEX16(avReg[15]));
		avReg[15]++;
		avReg[reg] = SEX8(v);
		CLRFLAGS();
	}
	
	private void fx_ibt_r0() { FX_IBT(0); }
	private void fx_ibt_r1() { FX_IBT(1); }
	private void fx_ibt_r2() { FX_IBT(2); }
	private void fx_ibt_r3() { FX_IBT(3); }
	private void fx_ibt_r4() { FX_IBT(4); }
	private void fx_ibt_r5() { FX_IBT(5); }
	private void fx_ibt_r6() { FX_IBT(6); }
	private void fx_ibt_r7() { FX_IBT(7); }
	private void fx_ibt_r8() { FX_IBT(8); }
	private void fx_ibt_r9() { FX_IBT(9); }
	private void fx_ibt_r10() { FX_IBT(10); }
	private void fx_ibt_r11() { FX_IBT(11); }
	private void fx_ibt_r12() { FX_IBT(12); }
	private void fx_ibt_r13() { FX_IBT(13); }
	private void fx_ibt_r14() { FX_IBT(14); READR14(); }
	private void fx_ibt_r15() { FX_IBT(15); }

	private void FX_LMS( int reg )
	{
		vLastRamAdr = vPipe << 1;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit(USEX16(avReg[15]));
		avReg[15]++;
		avReg[reg] = pvRamBank.get8Bit( USEX16( vLastRamAdr ) );
		avReg[reg] |= pvRamBank.get8Bit( USEX16( vLastRamAdr + 1 ) ) << 8;
		CLRFLAGS();
	}
	
	private void fx_lms_r0() { FX_LMS(0); }
	private void fx_lms_r1() { FX_LMS(1); }
	private void fx_lms_r2() { FX_LMS(2); }
	private void fx_lms_r3() { FX_LMS(3); }
	private void fx_lms_r4() { FX_LMS(4); }
	private void fx_lms_r5() { FX_LMS(5); }
	private void fx_lms_r6() { FX_LMS(6); }
	private void fx_lms_r7() { FX_LMS(7); }
	private void fx_lms_r8() { FX_LMS(8); }
	private void fx_lms_r9() { FX_LMS(9); }
	private void fx_lms_r10() { FX_LMS(10); }
	private void fx_lms_r11() { FX_LMS(11); }
	private void fx_lms_r12() { FX_LMS(12); }
	private void fx_lms_r13() { FX_LMS(13); }
	private void fx_lms_r14() { FX_LMS(14); READR14(); }
	private void fx_lms_r15() { FX_LMS(15); }

	private void FX_SMS( int reg )
	{
		int v = avReg[reg];
		vLastRamAdr = vPipe << 1;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit(USEX16(avReg[15]));
		pvRamBank.put8Bit( USEX16(vLastRamAdr), v & 0xFF );
		pvRamBank.put8Bit( USEX16(vLastRamAdr + 1), ( v >> 8 ) & 0xFF );
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_sms_r0() { FX_SMS(0); }
	private void fx_sms_r1() { FX_SMS(1); }
	private void fx_sms_r2() { FX_SMS(2); }
	private void fx_sms_r3() { FX_SMS(3); }
	private void fx_sms_r4() { FX_SMS(4); }
	private void fx_sms_r5() { FX_SMS(5); }
	private void fx_sms_r6() { FX_SMS(6); }
	private void fx_sms_r7() { FX_SMS(7); }
	private void fx_sms_r8() { FX_SMS(8); }
	private void fx_sms_r9() { FX_SMS(9); }
	private void fx_sms_r10() { FX_SMS(10); }
	private void fx_sms_r11() { FX_SMS(11); }
	private void fx_sms_r12() { FX_SMS(12); }
	private void fx_sms_r13() { FX_SMS(13); }
	private void fx_sms_r14() { FX_SMS(14); }
	private void fx_sms_r15() { FX_SMS(15); }

	private void FX_FROM(int reg)
	{
		if( ( vStatusReg & FLG_B ) == FLG_B )
		{
			int v = avReg[reg];
			avReg[15]++;
			DREG(v);
			vOverflow = ( v & 0x80 ) << 16;
			vSign = v;
			vZero = v;
			TESTR14();
			CLRFLAGS();
		}
		else
		{
			pvSreg = reg;
			avReg[15]++;
		}
	}
	private void fx_from_r0() { FX_FROM(0); }
	private void fx_from_r1() { FX_FROM(1); }
	private void fx_from_r2() { FX_FROM(2); }
	private void fx_from_r3() { FX_FROM(3); }
	private void fx_from_r4() { FX_FROM(4); }
	private void fx_from_r5() { FX_FROM(5); }
	private void fx_from_r6() { FX_FROM(6); }
	private void fx_from_r7() { FX_FROM(7); }
	private void fx_from_r8() { FX_FROM(8); }
	private void fx_from_r9() { FX_FROM(9); }
	private void fx_from_r10() { FX_FROM(10); }
	private void fx_from_r11() { FX_FROM(11); }
	private void fx_from_r12() { FX_FROM(12); }
	private void fx_from_r13() { FX_FROM(13); }
	private void fx_from_r14() { FX_FROM(14); }
	private void fx_from_r15() { FX_FROM(15); }

	// c0 - hib - move high-byte to low-byte
	private void fx_hib()
	{
		int v = USEX8( SREG() >> 8 );
		avReg[15]++;
		DREG( v );
		vSign = v << 8;
		vZero = v << 8;
		TESTR14();
		CLRFLAGS();
	}

	private void FX_OR( int reg)
	{
		int v = SREG() | avReg[reg];
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_or_r1() { FX_OR(1); }
	private void fx_or_r2() { FX_OR(2); }
	private void fx_or_r3() { FX_OR(3); }
	private void fx_or_r4() { FX_OR(4); }
	private void fx_or_r5() { FX_OR(5); }
	private void fx_or_r6() { FX_OR(6); }
	private void fx_or_r7() { FX_OR(7); }
	private void fx_or_r8() { FX_OR(8); }
	private void fx_or_r9() { FX_OR(9); }
	private void fx_or_r10() { FX_OR(10); }
	private void fx_or_r11() { FX_OR(11); }
	private void fx_or_r12() { FX_OR(12); }
	private void fx_or_r13() { FX_OR(13); }
	private void fx_or_r14() { FX_OR(14); }
	private void fx_or_r15() { FX_OR(15); }

	private void FX_XOR( int reg)
	{
		int v = SREG() ^ avReg[reg];
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_xor_r1() { FX_XOR(1); }
	private void fx_xor_r2() { FX_XOR(2); }
	private void fx_xor_r3() { FX_XOR(3); }
	private void fx_xor_r4() { FX_XOR(4); }
	private void fx_xor_r5() { FX_XOR(5); }
	private void fx_xor_r6() { FX_XOR(6); }
	private void fx_xor_r7() { FX_XOR(7); }
	private void fx_xor_r8() { FX_XOR(8); }
	private void fx_xor_r9() { FX_XOR(9); }
	private void fx_xor_r10() { FX_XOR(10); }
	private void fx_xor_r11() { FX_XOR(11); }
	private void fx_xor_r12() { FX_XOR(12); }
	private void fx_xor_r13() { FX_XOR(13); }
	private void fx_xor_r14() { FX_XOR(14); }
	private void fx_xor_r15() { FX_XOR(15); }

	private void FX_OR_I( int imm)
	{
		int v = SREG() | imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_or_i1() { FX_OR_I(1); }
	private void fx_or_i2() { FX_OR_I(2); }
	private void fx_or_i3() { FX_OR_I(3); }
	private void fx_or_i4() { FX_OR_I(4); }
	private void fx_or_i5() { FX_OR_I(5); }
	private void fx_or_i6() { FX_OR_I(6); }
	private void fx_or_i7() { FX_OR_I(7); }
	private void fx_or_i8() { FX_OR_I(8); }
	private void fx_or_i9() { FX_OR_I(9); }
	private void fx_or_i10() { FX_OR_I(10); }
	private void fx_or_i11() { FX_OR_I(11); }
	private void fx_or_i12() { FX_OR_I(12); }
	private void fx_or_i13() { FX_OR_I(13); }
	private void fx_or_i14() { FX_OR_I(14); }
	private void fx_or_i15() { FX_OR_I(15); }

	private void FX_XOR_I( int imm )
	{
		int v = SREG() ^ imm;
		avReg[15]++;
		DREG(v);
		vSign = v;
		vZero = v;
		TESTR14();
		CLRFLAGS();
	}
	
	private void fx_xor_i1() { FX_XOR_I(1); }
	private void fx_xor_i2() { FX_XOR_I(2); }
	private void fx_xor_i3() { FX_XOR_I(3); }
	private void fx_xor_i4() { FX_XOR_I(4); }
	private void fx_xor_i5() { FX_XOR_I(5); }
	private void fx_xor_i6() { FX_XOR_I(6); }
	private void fx_xor_i7() { FX_XOR_I(7); }
	private void fx_xor_i8() { FX_XOR_I(8); }
	private void fx_xor_i9() { FX_XOR_I(9); }
	private void fx_xor_i10() { FX_XOR_I(10); }
	private void fx_xor_i11() { FX_XOR_I(11); }
	private void fx_xor_i12() { FX_XOR_I(12); }
	private void fx_xor_i13() { FX_XOR_I(13); }
	private void fx_xor_i14() { FX_XOR_I(14); }
	private void fx_xor_i15() { FX_XOR_I(15); }

	private void FX_INC(int reg)
	{
		avReg[reg]++;
		vSign = avReg[reg];
		vZero = avReg[reg];
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_inc_r0() { FX_INC(0); }
	private void fx_inc_r1() { FX_INC(1); }
	private void fx_inc_r2() { FX_INC(2); }
	private void fx_inc_r3() { FX_INC(3); }
	private void fx_inc_r4() { FX_INC(4); }
	private void fx_inc_r5() { FX_INC(5); }
	private void fx_inc_r6() { FX_INC(6); }
	private void fx_inc_r7() { FX_INC(7); }
	private void fx_inc_r8() { FX_INC(8); }
	private void fx_inc_r9() { FX_INC(9); }
	private void fx_inc_r10() { FX_INC(10); }
	private void fx_inc_r11() { FX_INC(11); }
	private void fx_inc_r12() { FX_INC(12); }
	private void fx_inc_r13() { FX_INC(13); }
	private void fx_inc_r14() { FX_INC(14); READR14(); }
	

	private void fx_getc()
	{
		int c;
		
		if ( SuperFX.FX_DO_ROMBUFFER ) {
			c = vRomBuffer;
		} else {
			c = pvRomBank.get8Bit( USEX16( avReg[14] ) );
		}
				
		if( ( vPlotOptionReg & 0x04 ) != 0 )
			c = ( c & 0xf0) | ( c >> 4);
		
		if( ( vPlotOptionReg & 0x08 ) != 0 )
		{
			vColorReg &= 0xf0;
			vColorReg |= c & 0x0f;
		}
		else
		{
			vColorReg = USEX8(c);
		}
		CLRFLAGS();
		avReg[15]++;
	}

	private void fx_ramb()
	{
		vRamBankReg = SREG() & ( FX_RAM_BANKS - 1 );
		pvRamBank = apvRamBank[vRamBankReg & 0x3];
		CLRFLAGS();
		avReg[15]++;
	}

	private void fx_romb()
	{
		vRomBankReg = USEX8(SREG()) & 0x7f;
		pvRomBank = apvRomBank[vRomBankReg];
		CLRFLAGS();
		avReg[15]++;
	}

	private void FX_DEC(int reg)
	{
		avReg[reg]--;
		vSign = avReg[reg];
		vZero = avReg[reg];
		CLRFLAGS();
		avReg[15]++;
	}
	private void fx_dec_r0() { FX_DEC(0); }
	private void fx_dec_r1() { FX_DEC(1); }
	private void fx_dec_r2() { FX_DEC(2); }
	private void fx_dec_r3() { FX_DEC(3); }
	private void fx_dec_r4() { FX_DEC(4); }
	private void fx_dec_r5() { FX_DEC(5); }
	private void fx_dec_r6() { FX_DEC(6); }
	private void fx_dec_r7() { FX_DEC(7); }
	private void fx_dec_r8() { FX_DEC(8); }
	private void fx_dec_r9() { FX_DEC(9); }
	private void fx_dec_r10() { FX_DEC(10); }
	private void fx_dec_r11() { FX_DEC(11); }
	private void fx_dec_r12() { FX_DEC(12); }
	private void fx_dec_r13() { FX_DEC(13); }
	private void fx_dec_r14() { FX_DEC(14); READR14(); }

	private void fx_getb()
	{
		int v;
		
		if (SuperFX.FX_DO_ROMBUFFER)
		{
			v = vRomBuffer;
		} else {
			v = pvRomBank.get8Bit( USEX16( avReg[14] ) );
		}
		
		avReg[15]++;
		DREG( v );
		TESTR14();
		CLRFLAGS();
	}

	private void fx_getbh()
	{
		int v, c;
		
		if (SuperFX.FX_DO_ROMBUFFER)
		{
			c = USEX8(vRomBuffer);
		} else {
			c = pvRomBank.get8Bit( USEX16( avReg[14] ) );
		}

		v = USEX8( SREG() ) | (c<<8);
		avReg[15]++;
		DREG( v );
		TESTR14();
		CLRFLAGS();
	}

	// ef(ALT2) - getbl - get low-byte from ROM at address R14
	private void fx_getbl()
	{
		int v, c;
		
		if( SuperFX.FX_DO_ROMBUFFER )
		{
			c = USEX8(vRomBuffer);
		} else {
			c = pvRomBank.get8Bit( USEX16( avReg[14] ) );
		}

		v = ( SREG() & 0xff00 ) | c;
		
		avReg[15]++;
		DREG( v );
		TESTR14();
		CLRFLAGS();
	}

	private void fx_getbs()
	{
		int v;
		if( SuperFX.FX_DO_ROMBUFFER )
		{
			v = SEX8( vRomBuffer );

		} else {
			v = SEX8( pvRomBank.get8Bit( USEX16( (byte) avReg[14] ) ) );
		}

		avReg[15]++;
		DREG( v );
		TESTR14();
		CLRFLAGS();
	}

	private void FX_IWT( int reg) 
	{
		int v = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15]++;
		v |= USEX8(vPipe) << 8;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15]++;
		avReg[reg] = v;
		CLRFLAGS();
	}
	
	private void fx_iwt_r0() { FX_IWT(0); }
	private void fx_iwt_r1() { FX_IWT(1); }
	private void fx_iwt_r2() { FX_IWT(2); }
	private void fx_iwt_r3() { FX_IWT(3); }
	private void fx_iwt_r4() { FX_IWT(4); }
	private void fx_iwt_r5() { FX_IWT(5); }
	private void fx_iwt_r6() { FX_IWT(6); }
	private void fx_iwt_r7() { FX_IWT(7); }
	private void fx_iwt_r8() { FX_IWT(8); }
	private void fx_iwt_r9() { FX_IWT(9); }
	private void fx_iwt_r10() { FX_IWT(10); }
	private void fx_iwt_r11() { FX_IWT(11); }
	private void fx_iwt_r12() { FX_IWT(12); }
	private void fx_iwt_r13() { FX_IWT(13); }
	private void fx_iwt_r14() { FX_IWT(14); READR14(); }
	private void fx_iwt_r15() { FX_IWT(15); }

	private void FX_LM( int reg )
	{
		vLastRamAdr = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15]++;
		vLastRamAdr |= USEX8(vPipe) << 8;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15]++;
		avReg[reg] = pvRamBank.get8Bit( USEX16( vLastRamAdr ) );
		avReg[reg] |= USEX8( pvRamBank.get8Bit( USEX16( vLastRamAdr ^ 1 ) ) ) << 8;
		CLRFLAGS();
	}
	
	private void fx_lm_r0() { FX_LM(0); }
	private void fx_lm_r1() { FX_LM(1); }
	private void fx_lm_r2() { FX_LM(2); }
	private void fx_lm_r3() { FX_LM(3); }
	private void fx_lm_r4() { FX_LM(4); }
	private void fx_lm_r5() { FX_LM(5); }
	private void fx_lm_r6() { FX_LM(6); }
	private void fx_lm_r7() { FX_LM(7); }
	private void fx_lm_r8() { FX_LM(8); }
	private void fx_lm_r9() { FX_LM(9); }
	private void fx_lm_r10() { FX_LM(10); }
	private void fx_lm_r11() { FX_LM(11); }
	private void fx_lm_r12() { FX_LM(12); }
	private void fx_lm_r13() { FX_LM(13); }
	private void fx_lm_r14() { FX_LM(14); READR14(); }
	private void fx_lm_r15() { FX_LM(15); }


	private void FX_SM( int reg )
	{
		int v = avReg[reg];
		vLastRamAdr = vPipe;
		avReg[15]++;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		avReg[15]++;
		vLastRamAdr |= USEX8(vPipe) << 8;
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );
		
		pvRamBank.put8Bit( USEX16( vLastRamAdr ), v );
		pvRamBank.put8Bit( USEX16( vLastRamAdr ^ 1 ), v >> 8 );
				  
		CLRFLAGS();
		avReg[15]++;
	}
	
	private void fx_sm_r0() { FX_SM(0); }
	private void fx_sm_r1() { FX_SM(1); }
	private void fx_sm_r2() { FX_SM(2); }
	private void fx_sm_r3() { FX_SM(3); }
	private void fx_sm_r4() { FX_SM(4); }
	private void fx_sm_r5() { FX_SM(5); }
	private void fx_sm_r6() { FX_SM(6); }
	private void fx_sm_r7() { FX_SM(7); }
	private void fx_sm_r8() { FX_SM(8); }
	private void fx_sm_r9() { FX_SM(9); }
	private void fx_sm_r10() { FX_SM(10); }
	private void fx_sm_r11() { FX_SM(11); }
	private void fx_sm_r12() { FX_SM(12); }
	private void fx_sm_r13() { FX_SM(13); }
	private void fx_sm_r14() { FX_SM(14); }
	private void fx_sm_r15() { FX_SM(15); }
	
	
	private int fx_run(int nInstructions)
	{
		vCounter = nInstructions;
		READR14();
		
		while( ( vStatusReg & FLG_G ) == FLG_G && (vCounter-- > 0) )
		{
			FX_STEP();
		}

		return (nInstructions - vInstCount);
	}

	private int fx_run_to_breakpoint(int nInstructions)
	{
		int vCounter = 0;
		while( ( vStatusReg & FLG_G ) == FLG_G && vCounter < nInstructions)
		{
			vCounter++;
			FX_STEP();
		
			if( USEX16( avReg[15] ) == vBreakPoint)
			{
				vErrorCode = SuperFX.FX_BREAKPOINT;
				break;
			}
		}
		
		return vCounter;
	}

	private int fx_step_over(int nInstructions)
	{
		int vCounter = 0;
		
		while( ( vStatusReg & FLG_G ) == FLG_G  && vCounter < nInstructions)
		{
			vCounter++;
			
			FX_STEP();
			
			if( USEX16( avReg[15] ) == vBreakPoint)
			{
				vErrorCode = SuperFX.FX_BREAKPOINT;
				break;
			}
			
			if( USEX16( avReg[15] ) == vStepPoint)
			{
				break;
			}
		}
		
		return vCounter;
	}
	
	private void FX_STEP()
	{
		int vOpcode = vPipe;
		
		vPipe = pvPrgBank.get8Bit( USEX16( avReg[15] ) );

		switch ( (vStatusReg & 0x300) | vOpcode )
		{
		    // ALT0 Table
			case 0x00: fx_stop(); break;
			case 0x01: fx_nop(); break;
			case 0x02: fx_cache(); break;
			case 0x03: fx_lsr(); break;
			case 0x04: fx_rol(); break;
			case 0x05: fx_bra(); break;
			case 0x06: fx_bge(); break;
			case 0x07: fx_blt(); break;
			
			case 0x08: fx_bne(); break;
			case 0x09: fx_beq(); break;
			case 0x0A: fx_bpl(); break;
			case 0x0B: fx_bmi(); break;
			case 0x0C: fx_bcc(); break;
			case 0x0D: fx_bcs(); break;
			case 0x0E: fx_bvc(); break;
			case 0x0F: fx_bvs(); break;
			
			case 0x10: fx_to_r0(); break;
			case 0x11: fx_to_r1(); break;
			case 0x12: fx_to_r2(); break;
			case 0x13: fx_to_r3(); break;
			case 0x14: fx_to_r4(); break;
			case 0x15: fx_to_r5(); break;
			case 0x16: fx_to_r6(); break;
			case 0x17: fx_to_r7(); break;
			
			case 0x18: fx_to_r8(); break;
			case 0x19: fx_to_r9(); break;
			case 0x1A: fx_to_r10(); break;
			case 0x1B: fx_to_r11(); break;
			case 0x1C: fx_to_r12(); break;
			case 0x1D: fx_to_r13(); break;
			case 0x1E: fx_to_r14(); break;
			case 0x1F: fx_to_r15(); break;
			
			case 0x20: fx_with_r0(); break;
			case 0x21: fx_with_r1(); break;
			case 0x22: fx_with_r2(); break;
			case 0x23: fx_with_r3(); break;
			case 0x24: fx_with_r4(); break;
			case 0x25: fx_with_r5(); break;
			case 0x26: fx_with_r6(); break;
			case 0x27: fx_with_r7(); break;
			
			case 0x28: fx_with_r8(); break;
			case 0x29: fx_with_r9(); break;
			case 0x2A: fx_with_r10(); break;
			case 0x2B: fx_with_r11(); break;
			case 0x2C: fx_with_r12(); break;
			case 0x2D: fx_with_r13(); break;
			case 0x2E: fx_with_r14(); break;
			case 0x2F: fx_with_r15(); break;
			
			case 0x30: fx_stw_r0(); break;
			case 0x31: fx_stw_r1(); break;
			case 0x32: fx_stw_r2(); break;
			case 0x33: fx_stw_r3(); break;
			case 0x34: fx_stw_r4(); break;
			case 0x35: fx_stw_r5(); break;
			case 0x36: fx_stw_r6(); break;
			case 0x37: fx_stw_r7(); break;

			case 0x38: fx_stw_r8(); break;
			case 0x39: fx_stw_r9(); break;
			case 0x3a: fx_stw_r10(); break;
			case 0x3b: fx_stw_r11(); break;
			case 0x3c: fx_loop(); break;
			case 0x3d: fx_alt1(); break;
			case 0x3e: fx_alt2(); break;
			case 0x3f: fx_alt3(); break;


			case 0x40: fx_ldw_r0(); break;
			case 0x41: fx_ldw_r1(); break;
			case 0x42: fx_ldw_r2(); break;
			case 0x43: fx_ldw_r3(); break;
			case 0x44: fx_ldw_r4(); break;
			case 0x45: fx_ldw_r5(); break;
			case 0x46: fx_ldw_r6(); break;
			case 0x47: fx_ldw_r7(); break;

			case 0x48: fx_ldw_r8(); break;
			case 0x49: fx_ldw_r9(); break;
			case 0x4a: fx_ldw_r10(); break;
			case 0x4b: fx_ldw_r11(); break;
			case 0x4c: fx_plot(); break;
			case 0x4d: fx_swap(); break;
			case 0x4e: fx_color(); break;
			case 0x4f: fx_not(); break;

			case 0x50: fx_add_r0(); break;
			case 0x51: fx_add_r1(); break;
			case 0x52: fx_add_r2(); break;
			case 0x53: fx_add_r3(); break;
			case 0x54: fx_add_r4(); break;
			case 0x55: fx_add_r5(); break;
			case 0x56: fx_add_r6(); break;
			case 0x57: fx_add_r7(); break;

			case 0x58: fx_add_r8(); break;
			case 0x59: fx_add_r9(); break;
			case 0x5a: fx_add_r10(); break;
			case 0x5b: fx_add_r11(); break;
			case 0x5c: fx_add_r12(); break;
			case 0x5d: fx_add_r13(); break;
			case 0x5e: fx_add_r14(); break;
			case 0x5f: fx_add_r15(); break;

			case 0x60: fx_sub_r0(); break;
			case 0x61: fx_sub_r1(); break;
			case 0x62: fx_sub_r2(); break;
			case 0x63: fx_sub_r3(); break;
			case 0x64: fx_sub_r4(); break;
			case 0x65: fx_sub_r5(); break;
			case 0x66: fx_sub_r6(); break;
			case 0x67: fx_sub_r7(); break;

			case 0x68: fx_sub_r8(); break;
			case 0x69: fx_sub_r9(); break;
			case 0x6a: fx_sub_r10(); break;
			case 0x6b: fx_sub_r11(); break;
			case 0x6c: fx_sub_r12(); break;
			case 0x6d: fx_sub_r13(); break;
			case 0x6e: fx_sub_r14(); break;
			case 0x6f: fx_sub_r15(); break;

			case 0x70: fx_merge(); break;
			case 0x71: fx_and_r1(); break;
			case 0x72: fx_and_r2(); break;
			case 0x73: fx_and_r3(); break;
			case 0x74: fx_and_r4(); break;
			case 0x75: fx_and_r5(); break;
			case 0x76: fx_and_r6(); break;
			case 0x77: fx_and_r7(); break;

			case 0x78: fx_and_r8(); break;
			case 0x79: fx_and_r9(); break;
			case 0x7a: fx_and_r10(); break;
			case 0x7b: fx_and_r11(); break;
			case 0x7c: fx_and_r12(); break;
			case 0x7d: fx_and_r13(); break;
			case 0x7e: fx_and_r14(); break;
			case 0x7f: fx_and_r15(); break;

			// 80 - 8f
			case 0x80: fx_mult_r0(); break;
			case 0x81: fx_mult_r1(); break;
			case 0x82: fx_mult_r2(); break;
			case 0x83: fx_mult_r3(); break;
			case 0x84: fx_mult_r4(); break;
			case 0x85: fx_mult_r5(); break;
			case 0x86: fx_mult_r6(); break;
			case 0x87: fx_mult_r7(); break;

			case 0x88: fx_mult_r8(); break;
			case 0x89: fx_mult_r9(); break;
			case 0x8a: fx_mult_r10(); break;
			case 0x8b: fx_mult_r11(); break;
			case 0x8c: fx_mult_r12(); break;
			case 0x8d: fx_mult_r13(); break;
			case 0x8e: fx_mult_r14(); break;
			case 0x8f: fx_mult_r15(); break;

			// 90 - 9f
			case 0x90: fx_sbk(); break;
			case 0x91: fx_link_i1(); break;
			case 0x92: fx_link_i2(); break;
			case 0x93: fx_link_i3(); break;
			case 0x94: fx_link_i4(); break;
			case 0x95: fx_sex(); break;
			case 0x96: fx_asr(); break;
			case 0x97: fx_ror(); break;

			case 0x98: fx_jmp_r8(); break;
			case 0x99: fx_jmp_r9(); break;
			case 0x9a: fx_jmp_r10(); break;
			case 0x9b: fx_jmp_r11(); break;
			case 0x9c: fx_jmp_r12(); break;
			case 0x9d: fx_jmp_r13(); break;
			case 0x9e: fx_lob(); break;
			case 0x9f: fx_fmult(); break;

			// a0 - af
			case 0xa0: fx_ibt_r0(); break;
			case 0xa1: fx_ibt_r1(); break;
			case 0xa2: fx_ibt_r2(); break;
			case 0xa3: fx_ibt_r3(); break;
			case 0xa4: fx_ibt_r4(); break;
			case 0xa5: fx_ibt_r5(); break;
			case 0xa6: fx_ibt_r6(); break;
			case 0xa7: fx_ibt_r7(); break;

			case 0xa8: fx_ibt_r8(); break;
			case 0xa9: fx_ibt_r9(); break;
			case 0xaa: fx_ibt_r10(); break;
			case 0xab: fx_ibt_r11(); break;
			case 0xac: fx_ibt_r12(); break;
			case 0xad: fx_ibt_r13(); break;
			case 0xae: fx_ibt_r14(); break;
			case 0xaf: fx_ibt_r15(); break;

			// b0 - bf
			case 0xb0: fx_from_r0(); break;
			case 0xb1: fx_from_r1(); break;
			case 0xb2: fx_from_r2(); break;
			case 0xb3: fx_from_r3(); break;
			case 0xb4: fx_from_r4(); break;
			case 0xb5: fx_from_r5(); break;
			case 0xb6: fx_from_r6(); break;
			case 0xb7: fx_from_r7(); break;

			case 0xb8: fx_from_r8(); break;
			case 0xb9: fx_from_r9(); break;
			case 0xba: fx_from_r10(); break;
			case 0xbb: fx_from_r11(); break;
			case 0xbc: fx_from_r12(); break;
			case 0xbd: fx_from_r13(); break;
			case 0xbe: fx_from_r14(); break;
			case 0xbf: fx_from_r15(); break;

			// c0 - cf
			case 0xc0: fx_hib(); break;
			case 0xc1: fx_or_r1(); break;
			case 0xc2: fx_or_r2(); break;
			case 0xc3: fx_or_r3(); break;
			case 0xc4: fx_or_r4(); break;
			case 0xc5: fx_or_r5(); break;
			case 0xc6: fx_or_r6(); break;
			case 0xc7: fx_or_r7(); break;

			case 0xc8: fx_or_r8(); break;
			case 0xc9: fx_or_r9(); break;
			case 0xca: fx_or_r10(); break;
			case 0xcb: fx_or_r11(); break;
			case 0xcc: fx_or_r12(); break;
			case 0xcd: fx_or_r13(); break;
			case 0xce: fx_or_r14(); break;
			case 0xcf: fx_or_r15(); break;

			// d0 - df
			case 0xd0: fx_inc_r0(); break;
			case 0xd1: fx_inc_r1(); break;
			case 0xd2: fx_inc_r2(); break;
			case 0xd3: fx_inc_r3(); break;
			case 0xd4: fx_inc_r4(); break;
			case 0xd5: fx_inc_r5(); break;
			case 0xd6: fx_inc_r6(); break;
			case 0xd7: fx_inc_r7(); break;

			case 0xd8: fx_inc_r8(); break;
			case 0xd9: fx_inc_r9(); break;
			case 0xda: fx_inc_r10(); break;
			case 0xdb: fx_inc_r11(); break;
			case 0xdc: fx_inc_r12(); break;
			case 0xdd: fx_inc_r13(); break;
			case 0xde: fx_inc_r14(); break;
			case 0xdf: fx_getc(); break;

			// e0 - ef
			case 0xe0: fx_dec_r0(); break;
			case 0xe1: fx_dec_r1(); break;
			case 0xe2: fx_dec_r2(); break;
			case 0xe3: fx_dec_r3(); break;
			case 0xe4: fx_dec_r4(); break;
			case 0xe5: fx_dec_r5(); break;
			case 0xe6: fx_dec_r6(); break;
			case 0xe7: fx_dec_r7(); break;

			case 0xe8: fx_dec_r8(); break;
			case 0xe9: fx_dec_r9(); break;
			case 0xea: fx_dec_r10(); break;
			case 0xeb: fx_dec_r11(); break;
			case 0xec: fx_dec_r12(); break;
			case 0xed: fx_dec_r13(); break;
			case 0xee: fx_dec_r14(); break;
			case 0xef: fx_getb(); break;

			case 0xf0: fx_iwt_r0(); break;
			case 0xf1: fx_iwt_r1(); break;
			case 0xf2: fx_iwt_r2(); break;
			case 0xf3: fx_iwt_r3(); break;
			case 0xf4: fx_iwt_r4(); break;
			case 0xf5: fx_iwt_r5(); break;
			case 0xf6: fx_iwt_r6(); break;
			case 0xf7: fx_iwt_r7(); break;

			case 0xf8: fx_iwt_r8(); break;
			case 0xf9: fx_iwt_r9(); break;
			case 0xfa: fx_iwt_r10(); break;
			case 0xfb: fx_iwt_r11(); break;
			case 0xfc: fx_iwt_r12(); break;
			case 0xfd: fx_iwt_r13(); break;
			case 0xfe: fx_iwt_r14(); break;
			case 0xff: fx_iwt_r15(); break;

		    // ALT1 Table
		    
			case 0x100: fx_stop(); break;
			case 0x101: fx_nop(); break;
			case 0x102: fx_cache(); break;
			case 0x103: fx_lsr(); break;
			case 0x104: fx_rol(); break;
			case 0x105: fx_bra(); break;
			case 0x106: fx_bge(); break;
			case 0x107: fx_blt(); break;

			case 0x108: fx_bne(); break;
			case 0x109: fx_beq(); break;
			case 0x10a: fx_bpl(); break;
			case 0x10b: fx_bmi(); break;
			case 0x10c: fx_bcc(); break;
			case 0x10d: fx_bcs(); break;
			case 0x10e: fx_bvc(); break;
			case 0x10f: fx_bvs(); break;

			// 10 - 1f
			case 0x110: fx_to_r0(); break;
			case 0x111: fx_to_r1(); break;
			case 0x112: fx_to_r2(); break;
			case 0x113: fx_to_r3(); break;
			case 0x114: fx_to_r4(); break;
			case 0x115: fx_to_r5(); break;
			case 0x116: fx_to_r6(); break;
			case 0x117: fx_to_r7(); break;

			case 0x118: fx_to_r8(); break;
			case 0x119: fx_to_r9(); break;
			case 0x11a: fx_to_r10(); break;
			case 0x11b: fx_to_r11(); break;
			case 0x11c: fx_to_r12(); break;
			case 0x11d: fx_to_r13(); break;
			case 0x11e: fx_to_r14(); break;
			case 0x11f: fx_to_r15(); break;

			// 20 - 2f
			case 0x120: fx_with_r0(); break;
			case 0x121: fx_with_r1(); break;
			case 0x122: fx_with_r2(); break;
			case 0x123: fx_with_r3(); break;
			case 0x124: fx_with_r4(); break;
			case 0x125: fx_with_r5(); break;
			case 0x126: fx_with_r6(); break;
			case 0x127: fx_with_r7(); break;

			case 0x128: fx_with_r8(); break;
			case 0x129: fx_with_r9(); break;
			case 0x12a: fx_with_r10(); break;
			case 0x12b: fx_with_r11(); break;
			case 0x12c: fx_with_r12(); break;
			case 0x12d: fx_with_r13(); break;
			case 0x12e: fx_with_r14(); break;
			case 0x12f: fx_with_r15(); break;

			// 30 - 3f
			case 0x130: fx_stb_r0(); break;
			case 0x131: fx_stb_r1(); break;
			case 0x132: fx_stb_r2(); break;
			case 0x133: fx_stb_r3(); break;
			case 0x134: fx_stb_r4(); break;
			case 0x135: fx_stb_r5(); break;
			case 0x136: fx_stb_r6(); break;
			case 0x137: fx_stb_r7(); break;

			case 0x138: fx_stb_r8(); break;
			case 0x139: fx_stb_r9(); break;
			case 0x13a: fx_stb_r10(); break;
			case 0x13b: fx_stb_r11(); break;
			case 0x13c: fx_loop(); break;
			case 0x13d: fx_alt1(); break;
			case 0x13e: fx_alt2(); break;
			case 0x13f: fx_alt3(); break;

			// 40 - 4f
			case 0x140: fx_ldb_r0(); break;
			case 0x141: fx_ldb_r1(); break;
			case 0x142: fx_ldb_r2(); break;
			case 0x143: fx_ldb_r3(); break;
			case 0x144: fx_ldb_r4(); break;
			case 0x145: fx_ldb_r5(); break;
			case 0x146: fx_ldb_r6(); break;
			case 0x147: fx_ldb_r7(); break;

			case 0x148: fx_ldb_r8(); break;
			case 0x149: fx_ldb_r9(); break;
			case 0x14a: fx_ldb_r10(); break;
			case 0x14b: fx_ldb_r11(); break;
			case 0x14c: fx_rpix(); break;
			case 0x14d: fx_swap(); break;
			case 0x14e: fx_cmode(); break;
			case 0x14f: fx_not(); break;

			// 50 - 5f
			case 0x150: fx_adc_r0(); break;
			case 0x151: fx_adc_r1(); break;
			case 0x152: fx_adc_r2(); break;
			case 0x153: fx_adc_r3(); break;
			case 0x154: fx_adc_r4(); break;
			case 0x155: fx_adc_r5(); break;
			case 0x156: fx_adc_r6(); break;
			case 0x157: fx_adc_r7(); break;

			case 0x158: fx_adc_r8(); break;
			case 0x159: fx_adc_r9(); break;
			case 0x15a: fx_adc_r10(); break;
			case 0x15b: fx_adc_r11(); break;
			case 0x15c: fx_adc_r12(); break;
			case 0x15d: fx_adc_r13(); break;
			case 0x15e: fx_adc_r14(); break;
			case 0x15f: fx_adc_r15(); break;

			// 60 - 6f
			case 0x160: fx_sbc_r0(); break;
			case 0x161: fx_sbc_r1(); break;
			case 0x162: fx_sbc_r2(); break;
			case 0x163: fx_sbc_r3(); break;
			case 0x164: fx_sbc_r4(); break;
			case 0x165: fx_sbc_r5(); break;
			case 0x166: fx_sbc_r6(); break;
			case 0x167: fx_sbc_r7(); break;

			case 0x168: fx_sbc_r8(); break;
			case 0x169: fx_sbc_r9(); break;
			case 0x16a: fx_sbc_r10(); break;
			case 0x16b: fx_sbc_r11(); break;
			case 0x16c: fx_sbc_r12(); break;
			case 0x16d: fx_sbc_r13(); break;
			case 0x16e: fx_sbc_r14(); break;
			case 0x16f: fx_sbc_r15(); break;

			// 70 - 7f
			case 0x170: fx_merge(); break;
			case 0x171: fx_bic_r1(); break;
			case 0x172: fx_bic_r2(); break;
			case 0x173: fx_bic_r3(); break;
			case 0x174: fx_bic_r4(); break;
			case 0x175: fx_bic_r5(); break;
			case 0x176: fx_bic_r6(); break;
			case 0x177: fx_bic_r7(); break;

			case 0x178: fx_bic_r8(); break;
			case 0x179: fx_bic_r9(); break;
			case 0x17a: fx_bic_r10(); break;
			case 0x17b: fx_bic_r11(); break;
			case 0x17c: fx_bic_r12(); break;
			case 0x17d: fx_bic_r13(); break;
			case 0x17e: fx_bic_r14(); break;
			case 0x17f: fx_bic_r15(); break;

			// 80 - 8f
			case 0x180: fx_umult_r0(); break;
			case 0x181: fx_umult_r1(); break;
			case 0x182: fx_umult_r2(); break;
			case 0x183: fx_umult_r3(); break;
			case 0x184: fx_umult_r4(); break;
			case 0x185: fx_umult_r5(); break;
			case 0x186: fx_umult_r6(); break;
			case 0x187: fx_umult_r7(); break;

			case 0x188: fx_umult_r8(); break;
			case 0x189: fx_umult_r9(); break;
			case 0x18a: fx_umult_r10(); break;
			case 0x18b: fx_umult_r11(); break;
			case 0x18c: fx_umult_r12(); break;
			case 0x18d: fx_umult_r13(); break;
			case 0x18e: fx_umult_r14(); break;
			case 0x18f: fx_umult_r15(); break;

			// 90 - 9f
			case 0x190: fx_sbk(); break;
			case 0x191: fx_link_i1(); break;
			case 0x192: fx_link_i2(); break;
			case 0x193: fx_link_i3(); break;
			case 0x194: fx_link_i4(); break;
			case 0x195: fx_sex(); break;
			case 0x196: fx_div2(); break;
			case 0x197: fx_ror(); break;

			case 0x198: fx_ljmp_r8(); break;
			case 0x199: fx_ljmp_r9(); break;
			case 0x19a: fx_ljmp_r10(); break;
			case 0x19b: fx_ljmp_r11(); break;
			case 0x19c: fx_ljmp_r12(); break;
			case 0x19d: fx_ljmp_r13(); break;
			case 0x19e: fx_lob(); break;
			case 0x19f: fx_lmult(); break;

			// a0 - af
			case 0x1a0: fx_lms_r0(); break;
			case 0x1a1: fx_lms_r1(); break;
			case 0x1a2: fx_lms_r2(); break;
			case 0x1a3: fx_lms_r3(); break;
			case 0x1a4: fx_lms_r4(); break;
			case 0x1a5: fx_lms_r5(); break;
			case 0x1a6: fx_lms_r6(); break;
			case 0x1a7: fx_lms_r7(); break;

			case 0x1a8: fx_lms_r8(); break;
			case 0x1a9: fx_lms_r9(); break;
			case 0x1aa: fx_lms_r10(); break;
			case 0x1ab: fx_lms_r11(); break;
			case 0x1ac: fx_lms_r12(); break;
			case 0x1ad: fx_lms_r13(); break;
			case 0x1ae: fx_lms_r14(); break;
			case 0x1af: fx_lms_r15(); break;

			// b0 - bf
			case 0x1b0: fx_from_r0(); break;
			case 0x1b1: fx_from_r1(); break;
			case 0x1b2: fx_from_r2(); break;
			case 0x1b3: fx_from_r3(); break;
			case 0x1b4: fx_from_r4(); break;
			case 0x1b5: fx_from_r5(); break;
			case 0x1b6: fx_from_r6(); break;
			case 0x1b7: fx_from_r7(); break;

			case 0x1b8: fx_from_r8(); break;
			case 0x1b9: fx_from_r9(); break;
			case 0x1ba: fx_from_r10(); break;
			case 0x1bb: fx_from_r11(); break;
			case 0x1bc: fx_from_r12(); break;
			case 0x1bd: fx_from_r13(); break;
			case 0x1be: fx_from_r14(); break;
			case 0x1bf: fx_from_r15(); break;

			// c0 - cf
			case 0x1c0: fx_hib(); break;
			case 0x1c1: fx_xor_r1(); break;
			case 0x1c2: fx_xor_r2(); break;
			case 0x1c3: fx_xor_r3(); break;
			case 0x1c4: fx_xor_r4(); break;
			case 0x1c5: fx_xor_r5(); break;
			case 0x1c6: fx_xor_r6(); break;
			case 0x1c7: fx_xor_r7(); break;

			case 0x1c8: fx_xor_r8(); break;
			case 0x1c9: fx_xor_r9(); break;
			case 0x1ca: fx_xor_r10(); break;
			case 0x1cb: fx_xor_r11(); break;
			case 0x1cc: fx_xor_r12(); break;
			case 0x1cd: fx_xor_r13(); break;
			case 0x1ce: fx_xor_r14(); break;
			case 0x1cf: fx_xor_r15(); break;

			// d0 - df
			case 0x1d0: fx_inc_r0(); break;
			case 0x1d1: fx_inc_r1(); break;
			case 0x1d2: fx_inc_r2(); break;
			case 0x1d3: fx_inc_r3(); break;
			case 0x1d4: fx_inc_r4(); break;
			case 0x1d5: fx_inc_r5(); break;
			case 0x1d6: fx_inc_r6(); break;
			case 0x1d7: fx_inc_r7(); break;

			case 0x1d8: fx_inc_r8(); break;
			case 0x1d9: fx_inc_r9(); break;
			case 0x1da: fx_inc_r10(); break;
			case 0x1db: fx_inc_r11(); break;
			case 0x1dc: fx_inc_r12(); break;
			case 0x1dd: fx_inc_r13(); break;
			case 0x1de: fx_inc_r14(); break;
			case 0x1df: fx_getc(); break;

			// e0 - ef
			case 0x1e0: fx_dec_r0(); break;
			case 0x1e1: fx_dec_r1(); break;
			case 0x1e2: fx_dec_r2(); break;
			case 0x1e3: fx_dec_r3(); break;
			case 0x1e4: fx_dec_r4(); break;
			case 0x1e5: fx_dec_r5(); break;
			case 0x1e6: fx_dec_r6(); break;
			case 0x1e7: fx_dec_r7(); break;

			case 0x1e8: fx_dec_r8(); break;
			case 0x1e9: fx_dec_r9(); break;
			case 0x1ea: fx_dec_r10(); break;
			case 0x1eb: fx_dec_r11(); break;
			case 0x1ec: fx_dec_r12(); break;
			case 0x1ed: fx_dec_r13(); break;
			case 0x1ee: fx_dec_r14(); break;
			case 0x1ef: fx_getbh(); break;

			// f0 - ff
			case 0x1f0: fx_lm_r0(); break;
			case 0x1f1: fx_lm_r1(); break;
			case 0x1f2: fx_lm_r2(); break;
			case 0x1f3: fx_lm_r3(); break;
			case 0x1f4: fx_lm_r4(); break;
			case 0x1f5: fx_lm_r5(); break;
			case 0x1f6: fx_lm_r6(); break;
			case 0x1f7: fx_lm_r7(); break;

			case 0x1f8: fx_lm_r8(); break;
			case 0x1f9: fx_lm_r9(); break;
			case 0x1fa: fx_lm_r10(); break;
			case 0x1fb: fx_lm_r11(); break;
			case 0x1fc: fx_lm_r12(); break;
			case 0x1fd: fx_lm_r13(); break;
			case 0x1fe: fx_lm_r14(); break;
			case 0x1ff: fx_lm_r15(); break;

			// ALT2 Table

			// 00 - 0f
			case 0x200: fx_stop(); break;
			case 0x201: fx_nop(); break;
			case 0x202: fx_cache(); break;
			case 0x203: fx_lsr(); break;
			case 0x204: fx_rol(); break;
			case 0x205: fx_bra(); break;
			case 0x206: fx_bge(); break;
			case 0x207: fx_blt(); break;

			case 0x208: fx_bne(); break;
			case 0x209: fx_beq(); break;
			case 0x20a: fx_bpl(); break;
			case 0x20b: fx_bmi(); break;
			case 0x20c: fx_bcc(); break;
			case 0x20d: fx_bcs(); break;
			case 0x20e: fx_bvc(); break;
			case 0x20f: fx_bvs(); break;

			// 10 - 1f
			case 0x210: fx_to_r0(); break;
			case 0x211: fx_to_r1(); break;
			case 0x212: fx_to_r2(); break;
			case 0x213: fx_to_r3(); break;
			case 0x214: fx_to_r4(); break;
			case 0x215: fx_to_r5(); break;
			case 0x216: fx_to_r6(); break;
			case 0x217: fx_to_r7(); break;

			case 0x218: fx_to_r8(); break;
			case 0x219: fx_to_r9(); break;
			case 0x21a: fx_to_r10(); break;
			case 0x21b: fx_to_r11(); break;
			case 0x21c: fx_to_r12(); break;
			case 0x21d: fx_to_r13(); break;
			case 0x21e: fx_to_r14(); break;
			case 0x21f: fx_to_r15(); break;

			// 20 - 2f
			case 0x220: fx_with_r0(); break;
			case 0x221: fx_with_r1(); break;
			case 0x222: fx_with_r2(); break;
			case 0x223: fx_with_r3(); break;
			case 0x224: fx_with_r4(); break;
			case 0x225: fx_with_r5(); break;
			case 0x226: fx_with_r6(); break;
			case 0x227: fx_with_r7(); break;

			case 0x228: fx_with_r8(); break;
			case 0x229: fx_with_r9(); break;
			case 0x22a: fx_with_r10(); break;
			case 0x22b: fx_with_r11(); break;
			case 0x22c: fx_with_r12(); break;
			case 0x22d: fx_with_r13(); break;
			case 0x22e: fx_with_r14(); break;
			case 0x22f: fx_with_r15(); break;

			// 30 - 3f
			case 0x230: fx_stw_r0(); break;
			case 0x231: fx_stw_r1(); break;
			case 0x232: fx_stw_r2(); break;
			case 0x233: fx_stw_r3(); break;
			case 0x234: fx_stw_r4(); break;
			case 0x235: fx_stw_r5(); break;
			case 0x236: fx_stw_r6(); break;
			case 0x237: fx_stw_r7(); break;

			case 0x238: fx_stw_r8(); break;
			case 0x239: fx_stw_r9(); break;
			case 0x23a: fx_stw_r10(); break;
			case 0x23b: fx_stw_r11(); break;
			case 0x23c: fx_loop(); break;
			case 0x23d: fx_alt1(); break;
			case 0x23e: fx_alt2(); break;
			case 0x23f: fx_alt3(); break;

			// 40 - 4f
			case 0x240: fx_ldw_r0(); break;
			case 0x241: fx_ldw_r1(); break;
			case 0x242: fx_ldw_r2(); break;
			case 0x243: fx_ldw_r3(); break;
			case 0x244: fx_ldw_r4(); break;
			case 0x245: fx_ldw_r5(); break;
			case 0x246: fx_ldw_r6(); break;
			case 0x247: fx_ldw_r7(); break;

			case 0x248: fx_ldw_r8(); break;
			case 0x249: fx_ldw_r9(); break;
			case 0x24a: fx_ldw_r10(); break;
			case 0x24b: fx_ldw_r11(); break;
			case 0x24c: fx_plot(); break;
			case 0x24d: fx_swap(); break;
			case 0x24e: fx_color(); break;
			case 0x24f: fx_not(); break;

			// 50 - 5f
			case 0x250: fx_add_i0(); break;
			case 0x251: fx_add_i1(); break;
			case 0x252: fx_add_i2(); break;
			case 0x253: fx_add_i3(); break;
			case 0x254: fx_add_i4(); break;
			case 0x255: fx_add_i5(); break;
			case 0x256: fx_add_i6(); break;
			case 0x257: fx_add_i7(); break;

			case 0x258: fx_add_i8(); break;
			case 0x259: fx_add_i9(); break;
			case 0x25a: fx_add_i10(); break;
			case 0x25b: fx_add_i11(); break;
			case 0x25c: fx_add_i12(); break;
			case 0x25d: fx_add_i13(); break;
			case 0x25e: fx_add_i14(); break;
			case 0x25f: fx_add_i15(); break;

			// 60 - 6f
			case 0x260: fx_sub_i0(); break;
			case 0x261: fx_sub_i1(); break;
			case 0x262: fx_sub_i2(); break;
			case 0x263: fx_sub_i3(); break;
			case 0x264: fx_sub_i4(); break;
			case 0x265: fx_sub_i5(); break;
			case 0x266: fx_sub_i6(); break;
			case 0x267: fx_sub_i7(); break;

			case 0x268: fx_sub_i8(); break;
			case 0x269: fx_sub_i9(); break;
			case 0x26a: fx_sub_i10(); break;
			case 0x26b: fx_sub_i11(); break;
			case 0x26c: fx_sub_i12(); break;
			case 0x26d: fx_sub_i13(); break;
			case 0x26e: fx_sub_i14(); break;
			case 0x26f: fx_sub_i15(); break;

			// 70 - 7f
			case 0x270: fx_merge(); break;
			case 0x271: fx_and_i1(); break;
			case 0x272: fx_and_i2(); break;
			case 0x273: fx_and_i3(); break;
			case 0x274: fx_and_i4(); break;
			case 0x275: fx_and_i5(); break;
			case 0x276: fx_and_i6(); break;
			case 0x277: fx_and_i7(); break;

			case 0x278: fx_and_i8(); break;
			case 0x279: fx_and_i9(); break;
			case 0x27a: fx_and_i10(); break;
			case 0x27b: fx_and_i11(); break;
			case 0x27c: fx_and_i12(); break;
			case 0x27d: fx_and_i13(); break;
			case 0x27e: fx_and_i14(); break;
			case 0x27f: fx_and_i15(); break;

			// 80 - 8f
			case 0x280: fx_mult_i0(); break;
			case 0x281: fx_mult_i1(); break;
			case 0x282: fx_mult_i2(); break;
			case 0x283: fx_mult_i3(); break;
			case 0x284: fx_mult_i4(); break;
			case 0x285: fx_mult_i5(); break;
			case 0x286: fx_mult_i6(); break;
			case 0x287: fx_mult_i7(); break;

			case 0x288: fx_mult_i8(); break;
			case 0x289: fx_mult_i9(); break;
			case 0x28a: fx_mult_i10(); break;
			case 0x28b: fx_mult_i11(); break;
			case 0x28c: fx_mult_i12(); break;
			case 0x28d: fx_mult_i13(); break;
			case 0x28e: fx_mult_i14(); break;
			case 0x28f: fx_mult_i15(); break;

			// 90 - 9f
			case 0x290: fx_sbk(); break;
			case 0x291: fx_link_i1(); break;
			case 0x292: fx_link_i2(); break;
			case 0x293: fx_link_i3(); break;
			case 0x294: fx_link_i4(); break;
			case 0x295: fx_sex(); break;
			case 0x296: fx_asr(); break;
			case 0x297: fx_ror(); break;

			case 0x298: fx_jmp_r8(); break;
			case 0x299: fx_jmp_r9(); break;
			case 0x29a: fx_jmp_r10(); break;
			case 0x29b: fx_jmp_r11(); break;
			case 0x29c: fx_jmp_r12(); break;
			case 0x29d: fx_jmp_r13(); break;
			case 0x29e: fx_lob(); break;
			case 0x29f: fx_fmult(); break;

			// a0 - af
			case 0x2a0: fx_sms_r0(); break;
			case 0x2a1: fx_sms_r1(); break;
			case 0x2a2: fx_sms_r2(); break;
			case 0x2a3: fx_sms_r3(); break;
			case 0x2a4: fx_sms_r4(); break;
			case 0x2a5: fx_sms_r5(); break;
			case 0x2a6: fx_sms_r6(); break;
			case 0x2a7: fx_sms_r7(); break;

			case 0x2a8: fx_sms_r8(); break;
			case 0x2a9: fx_sms_r9(); break;
			case 0x2aa: fx_sms_r10(); break;
			case 0x2ab: fx_sms_r11(); break;
			case 0x2ac: fx_sms_r12(); break;
			case 0x2ad: fx_sms_r13(); break;
			case 0x2ae: fx_sms_r14(); break;
			case 0x2af: fx_sms_r15(); break;

			// b0 - bf
			case 0x2b0: fx_from_r0(); break;
			case 0x2b1: fx_from_r1(); break;
			case 0x2b2: fx_from_r2(); break;
			case 0x2b3: fx_from_r3(); break;
			case 0x2b4: fx_from_r4(); break;
			case 0x2b5: fx_from_r5(); break;
			case 0x2b6: fx_from_r6(); break;
			case 0x2b7: fx_from_r7(); break;

			case 0x2b8: fx_from_r8(); break;
			case 0x2b9: fx_from_r9(); break;
			case 0x2ba: fx_from_r10(); break;
			case 0x2bb: fx_from_r11(); break;
			case 0x2bc: fx_from_r12(); break;
			case 0x2bd: fx_from_r13(); break;
			case 0x2be: fx_from_r14(); break;
			case 0x2bf: fx_from_r15(); break;

			// c0 - cf
			case 0x2c0: fx_hib(); break;
			case 0x2c1: fx_or_i1(); break;
			case 0x2c2: fx_or_i2(); break;
			case 0x2c3: fx_or_i3(); break;
			case 0x2c4: fx_or_i4(); break;
			case 0x2c5: fx_or_i5(); break;
			case 0x2c6: fx_or_i6(); break;
			case 0x2c7: fx_or_i7(); break;

			case 0x2c8: fx_or_i8(); break;
			case 0x2c9: fx_or_i9(); break;
			case 0x2ca: fx_or_i10(); break;
			case 0x2cb: fx_or_i11(); break;
			case 0x2cc: fx_or_i12(); break;
			case 0x2cd: fx_or_i13(); break;
			case 0x2ce: fx_or_i14(); break;
			case 0x2cf: fx_or_i15(); break;

			// d0 - df
			case 0x2d0: fx_inc_r0(); break;
			case 0x2d1: fx_inc_r1(); break;
			case 0x2d2: fx_inc_r2(); break;
			case 0x2d3: fx_inc_r3(); break;
			case 0x2d4: fx_inc_r4(); break;
			case 0x2d5: fx_inc_r5(); break;
			case 0x2d6: fx_inc_r6(); break;
			case 0x2d7: fx_inc_r7(); break;

			case 0x2d8: fx_inc_r8(); break;
			case 0x2d9: fx_inc_r9(); break;
			case 0x2da: fx_inc_r10(); break;
			case 0x2db: fx_inc_r11(); break;
			case 0x2dc: fx_inc_r12(); break;
			case 0x2dd: fx_inc_r13(); break;
			case 0x2de: fx_inc_r14(); break;
			case 0x2df: fx_ramb(); break;

			// e0 - ef
			case 0x2e0: fx_dec_r0(); break;
			case 0x2e1: fx_dec_r1(); break;
			case 0x2e2: fx_dec_r2(); break;
			case 0x2e3: fx_dec_r3(); break;
			case 0x2e4: fx_dec_r4(); break;
			case 0x2e5: fx_dec_r5(); break;
			case 0x2e6: fx_dec_r6(); break;
			case 0x2e7: fx_dec_r7(); break;

			case 0x2e8: fx_dec_r8(); break;
			case 0x2e9: fx_dec_r9(); break;
			case 0x2ea: fx_dec_r10(); break;
			case 0x2eb: fx_dec_r11(); break;
			case 0x2ec: fx_dec_r12(); break;
			case 0x2ed: fx_dec_r13(); break;
			case 0x2ee: fx_dec_r14(); break;
			case 0x2ef: fx_getbl(); break;

			// f0 - ff
			case 0x2f0: fx_sm_r0(); break;
			case 0x2f1: fx_sm_r1(); break;
			case 0x2f2: fx_sm_r2(); break;
			case 0x2f3: fx_sm_r3(); break;
			case 0x2f4: fx_sm_r4(); break;
			case 0x2f5: fx_sm_r5(); break;
			case 0x2f6: fx_sm_r6(); break;
			case 0x2f7: fx_sm_r7(); break;

			case 0x2f8: fx_sm_r8(); break;
			case 0x2f9: fx_sm_r9(); break;
			case 0x2fa: fx_sm_r10(); break;
			case 0x2fb: fx_sm_r11(); break;
			case 0x2fc: fx_sm_r12(); break;
			case 0x2fd: fx_sm_r13(); break;
			case 0x2fe: fx_sm_r14(); break;
			case 0x2ff: fx_sm_r15(); break;


			case 0x300: fx_stop(); break;
			case 0x301: fx_nop(); break;
			case 0x302: fx_cache(); break;
			case 0x303: fx_lsr(); break;
			case 0x304: fx_rol(); break;
			case 0x305: fx_bra(); break;
			case 0x306: fx_bge(); break;
			case 0x307: fx_blt(); break;

			case 0x308: fx_bne(); break;
			case 0x309: fx_beq(); break;
			case 0x30a: fx_bpl(); break;
			case 0x30b: fx_bmi(); break;
			case 0x30c: fx_bcc(); break;
			case 0x30d: fx_bcs(); break;
			case 0x30e: fx_bvc(); break;
			case 0x30f: fx_bvs(); break;


			case 0x310: fx_to_r0(); break;
			case 0x311: fx_to_r1(); break;
			case 0x312: fx_to_r2(); break;
			case 0x313: fx_to_r3(); break;
			case 0x314: fx_to_r4(); break;
			case 0x315: fx_to_r5(); break;
			case 0x316: fx_to_r6(); break;
			case 0x317: fx_to_r7(); break;

			case 0x318: fx_to_r8(); break;
			case 0x319: fx_to_r9(); break;
			case 0x31a: fx_to_r10(); break;
			case 0x31b: fx_to_r11(); break;
			case 0x31c: fx_to_r12(); break;
			case 0x31d: fx_to_r13(); break;
			case 0x31e: fx_to_r14(); break;
			case 0x31f: fx_to_r15(); break;


			case 0x320: fx_with_r0(); break;
			case 0x321: fx_with_r1(); break;
			case 0x322: fx_with_r2(); break;
			case 0x323: fx_with_r3(); break;
			case 0x324: fx_with_r4(); break;
			case 0x325: fx_with_r5(); break;
			case 0x326: fx_with_r6(); break;
			case 0x327: fx_with_r7(); break;

			case 0x328: fx_with_r8(); break;
			case 0x329: fx_with_r9(); break;
			case 0x32a: fx_with_r10(); break;
			case 0x32b: fx_with_r11(); break;
			case 0x32c: fx_with_r12(); break;
			case 0x32d: fx_with_r13(); break;
			case 0x32e: fx_with_r14(); break;
			case 0x32f: fx_with_r15(); break;

			case 0x330: fx_stb_r0(); break;
			case 0x331: fx_stb_r1(); break;
			case 0x332: fx_stb_r2(); break;
			case 0x333: fx_stb_r3(); break;
			case 0x334: fx_stb_r4(); break;
			case 0x335: fx_stb_r5(); break;
			case 0x336: fx_stb_r6(); break;
			case 0x337: fx_stb_r7(); break;

			case 0x338: fx_stb_r8(); break;
			case 0x339: fx_stb_r9(); break;
			case 0x33a: fx_stb_r10(); break;
			case 0x33b: fx_stb_r11(); break;
			case 0x33c: fx_loop(); break;
			case 0x33d: fx_alt1(); break;
			case 0x33e: fx_alt2(); break;
			case 0x33f: fx_alt3(); break;


			case 0x340: fx_ldb_r0(); break;
			case 0x341: fx_ldb_r1(); break;
			case 0x342: fx_ldb_r2(); break;
			case 0x343: fx_ldb_r3(); break;
			case 0x344: fx_ldb_r4(); break;
			case 0x345: fx_ldb_r5(); break;
			case 0x346: fx_ldb_r6(); break;
			case 0x347: fx_ldb_r7(); break;

			case 0x348: fx_ldb_r8(); break;
			case 0x349: fx_ldb_r9(); break;
			case 0x34a: fx_ldb_r10(); break;
			case 0x34b: fx_ldb_r11(); break;
			case 0x34c: fx_rpix(); break;
			case 0x34d: fx_swap(); break;
			case 0x34e: fx_cmode(); break;
			case 0x34f: fx_not(); break;


			case 0x350: fx_adc_i0(); break;
			case 0x351: fx_adc_i1(); break;
			case 0x352: fx_adc_i2(); break;
			case 0x353: fx_adc_i3(); break;
			case 0x354: fx_adc_i4(); break;
			case 0x355: fx_adc_i5(); break;
			case 0x356: fx_adc_i6(); break;
			case 0x357: fx_adc_i7(); break;

			case 0x358: fx_adc_i8(); break;
			case 0x359: fx_adc_i9(); break;
			case 0x35a: fx_adc_i10(); break;
			case 0x35b: fx_adc_i11(); break;
			case 0x35c: fx_adc_i12(); break;
			case 0x35d: fx_adc_i13(); break;
			case 0x35e: fx_adc_i14(); break;
			case 0x35f: fx_adc_i15(); break;


			case 0x360: fx_cmp_r0(); break;
			case 0x361: fx_cmp_r1(); break;
			case 0x362: fx_cmp_r2(); break;
			case 0x363: fx_cmp_r3(); break;
			case 0x364: fx_cmp_r4(); break;
			case 0x365: fx_cmp_r5(); break;
			case 0x366: fx_cmp_r6(); break;
			case 0x367: fx_cmp_r7(); break;

			case 0x368: fx_cmp_r8(); break;
			case 0x369: fx_cmp_r9(); break;
			case 0x36a: fx_cmp_r10(); break;
			case 0x36b: fx_cmp_r11(); break;
			case 0x36c: fx_cmp_r12(); break;
			case 0x36d: fx_cmp_r13(); break;
			case 0x36e: fx_cmp_r14(); break;
			case 0x36f: fx_cmp_r15(); break;


			case 0x370: fx_merge(); break;
			case 0x371: fx_bic_i1(); break;
			case 0x372: fx_bic_i2(); break;
			case 0x373: fx_bic_i3(); break;
			case 0x374: fx_bic_i4(); break;
			case 0x375: fx_bic_i5(); break;
			case 0x376: fx_bic_i6(); break;
			case 0x377: fx_bic_i7(); break;

			case 0x378: fx_bic_i8(); break;
			case 0x379: fx_bic_i9(); break;
			case 0x37a: fx_bic_i10(); break;
			case 0x37b: fx_bic_i11(); break;
			case 0x37c: fx_bic_i12(); break;
			case 0x37d: fx_bic_i13(); break;
			case 0x37e: fx_bic_i14(); break;
			case 0x37f: fx_bic_i15(); break;


			case 0x380: fx_umult_i0(); break;
			case 0x381: fx_umult_i1(); break;
			case 0x382: fx_umult_i2(); break;
			case 0x383: fx_umult_i3(); break;
			case 0x384: fx_umult_i4(); break;
			case 0x385: fx_umult_i5(); break;
			case 0x386: fx_umult_i6(); break;
			case 0x387: fx_umult_i7(); break;

			case 0x388: fx_umult_i8(); break;
			case 0x389: fx_umult_i9(); break;
			case 0x38a: fx_umult_i10(); break;
			case 0x38b: fx_umult_i11(); break;
			case 0x38c: fx_umult_i12(); break;
			case 0x38d: fx_umult_i13(); break;
			case 0x38e: fx_umult_i14(); break;
			case 0x38f: fx_umult_i15(); break;


			case 0x390: fx_sbk(); break;
			case 0x391: fx_link_i1(); break;
			case 0x392: fx_link_i2(); break;
			case 0x393: fx_link_i3(); break;
			case 0x394: fx_link_i4(); break;
			case 0x395: fx_sex(); break;
			case 0x396: fx_div2(); break;
			case 0x397: fx_ror(); break;

			case 0x398: fx_ljmp_r8(); break;
			case 0x399: fx_ljmp_r9(); break;
			case 0x39a: fx_ljmp_r10(); break;
			case 0x39b: fx_ljmp_r11(); break;
			case 0x39c: fx_ljmp_r12(); break;
			case 0x39d: fx_ljmp_r13(); break;
			case 0x39e: fx_lob(); break;
			case 0x39f: fx_lmult(); break;

			case 0x3a0: fx_lms_r0(); break;
			case 0x3a1: fx_lms_r1(); break;
			case 0x3a2: fx_lms_r2(); break;
			case 0x3a3: fx_lms_r3(); break;
			case 0x3a4: fx_lms_r4(); break;
			case 0x3a5: fx_lms_r5(); break;
			case 0x3a6: fx_lms_r6(); break;
			case 0x3a7: fx_lms_r7(); break;

			case 0x3a8: fx_lms_r8(); break;
			case 0x3a9: fx_lms_r9(); break;
			case 0x3aa: fx_lms_r10(); break;
			case 0x3ab: fx_lms_r11(); break;
			case 0x3ac: fx_lms_r12(); break;
			case 0x3ad: fx_lms_r13(); break;
			case 0x3ae: fx_lms_r14(); break;
			case 0x3af: fx_lms_r15(); break;


			case 0x3b0: fx_from_r0(); break;
			case 0x3b1: fx_from_r1(); break;
			case 0x3b2: fx_from_r2(); break;
			case 0x3b3: fx_from_r3(); break;
			case 0x3b4: fx_from_r4(); break;
			case 0x3b5: fx_from_r5(); break;
			case 0x3b6: fx_from_r6(); break;
			case 0x3b7: fx_from_r7(); break;

			case 0x3b8: fx_from_r8(); break;
			case 0x3b9: fx_from_r9(); break;
			case 0x3ba: fx_from_r10(); break;
			case 0x3bb: fx_from_r11(); break;
			case 0x3bc: fx_from_r12(); break;
			case 0x3bd: fx_from_r13(); break;
			case 0x3be: fx_from_r14(); break;
			case 0x3bf: fx_from_r15(); break;

			case 0x3c0: fx_hib(); break;
			case 0x3c1: fx_xor_i1(); break;
			case 0x3c2: fx_xor_i2(); break;
			case 0x3c3: fx_xor_i3(); break;
			case 0x3c4: fx_xor_i4(); break;
			case 0x3c5: fx_xor_i5(); break;
			case 0x3c6: fx_xor_i6(); break;
			case 0x3c7: fx_xor_i7(); break;

			case 0x3c8: fx_xor_i8(); break;
			case 0x3c9: fx_xor_i9(); break;
			case 0x3ca: fx_xor_i10(); break;
			case 0x3cb: fx_xor_i11(); break;
			case 0x3cc: fx_xor_i12(); break;
			case 0x3cd: fx_xor_i13(); break;
			case 0x3ce: fx_xor_i14(); break;
			case 0x3cf: fx_xor_i15(); break;


			case 0x3d0: fx_inc_r0(); break;
			case 0x3d1: fx_inc_r1(); break;
			case 0x3d2: fx_inc_r2(); break;
			case 0x3d3: fx_inc_r3(); break;
			case 0x3d4: fx_inc_r4(); break;
			case 0x3d5: fx_inc_r5(); break;
			case 0x3d6: fx_inc_r6(); break;
			case 0x3d7: fx_inc_r7(); break;

			case 0x3d8: fx_inc_r8(); break;
			case 0x3d9: fx_inc_r9(); break;
			case 0x3da: fx_inc_r10(); break;
			case 0x3db: fx_inc_r11(); break;
			case 0x3dc: fx_inc_r12(); break;
			case 0x3dd: fx_inc_r13(); break;
			case 0x3de: fx_inc_r14(); break;
			case 0x3df: fx_romb(); break;

			case 0x3e0: fx_dec_r0(); break;
			case 0x3e1: fx_dec_r1(); break;
			case 0x3e2: fx_dec_r2(); break;
			case 0x3e3: fx_dec_r3(); break;
			case 0x3e4: fx_dec_r4(); break;
			case 0x3e5: fx_dec_r5(); break;
			case 0x3e6: fx_dec_r6(); break;
			case 0x3e7: fx_dec_r7(); break;

			case 0x3e8: fx_dec_r8(); break;
			case 0x3e9: fx_dec_r9(); break;
			case 0x3ea: fx_dec_r10(); break;
			case 0x3eb: fx_dec_r11(); break;
			case 0x3ec: fx_dec_r12(); break;
			case 0x3ed: fx_dec_r13(); break;
			case 0x3ee: fx_dec_r14(); break;
			case 0x3ef: fx_getbs(); break;

			case 0x3f0: fx_lm_r0(); break;
			case 0x3f1: fx_lm_r1(); break;
			case 0x3f2: fx_lm_r2(); break;
			case 0x3f3: fx_lm_r3(); break;
			case 0x3f4: fx_lm_r4(); break;
			case 0x3f5: fx_lm_r5(); break;
			case 0x3f6: fx_lm_r6(); break;
			case 0x3f7: fx_lm_r7(); break;

			case 0x3f8: fx_lm_r8(); break;
			case 0x3f9: fx_lm_r9(); break;
			case 0x3fa: fx_lm_r10(); break;
			case 0x3fb: fx_lm_r11(); break;
			case 0x3fc: fx_lm_r12(); break;
			case 0x3fd: fx_lm_r13(); break;
			case 0x3fe: fx_lm_r14(); break;
			case 0x3ff: fx_lm_r15(); break;					
		}
	}
	
	private int SREG()
	{
		return avReg[pvSreg];
	}

	private void SREG(int value)
	{
		avReg[pvSreg] = value;
	}
	
	private int DREG()
	{
		return avReg[pvDreg];
	}

	private void DREG(int value)
	{
		avReg[pvDreg] = value;
	}
	
	private void CLRFLAGS()
	{
		vStatusReg &= ~(FLG_ALT1|FLG_ALT2|FLG_B);
		pvDreg = 0;
		pvSreg = 0;
	}
	
	// Read R14
	private void READR14()
	{
		vRomBuffer = ROM(avReg[14]);
	}

	// Test and/or read R14
	private void TESTR14()
	{
		if( pvDreg == 14)
			READR14();
	}
	
	private int ROM(int idx)
	{
		return pvRomBank.get8Bit( USEX16(idx) );
	}
	
	// Sign extend from 8/16 bit to 32 bit
	private int SEX16( int a)
	{
		// SEX16(a) ((int32)((int16)(a)))
		return (int)((short) a );
	}
	
	private int SEX8( int a)
	{
		// SEX8(a) ((int32)((int8)(a)))
		return (int)((byte) a );
	}

	// Unsign extend from 8/16 bit to 32 bit
	private int USEX16(int a)
	{
		//NAC: ((uint32)((uint16)(a)));
		return a & 0xFFFF;
	}
	
	private int USEX8(int a)
	{
		//NAC: ((uint32)((uint8)(a)));
		return a & 0xFF;
	}

	private int SUSEX16(int a)
	{
		// NAC: ((int32)((uint16)(a)));
		return a & 0xFFFF;
	}
	
	void FxCacheWriteAccess(int vAddress)
	{
		if((vAddress & 0xf) != 0 )
			vCacheFlags |= 1 << ((vAddress&0x1f0) >> 4);
	}

	void FxFlushCache()
	{
		vCacheFlags = 0;
		vCacheBaseReg = 0;
		bCacheActive = 0;
	}

	private void fx_backupCache()
	{

	}

	private void fx_restoreCache()
	{

	}

	private void fx_flushCache()
	{
		fx_restoreCache();
		vCacheFlags = 0;
		bCacheActive = 0;
	}

	void fx_updateRamBank( int Byte)
	{
		// Update BankReg and Bank pointer
		vRamBankReg = Byte & (FX_RAM_BANKS-1);
		pvRamBank = apvRamBank[Byte & 0x3];
	}


	private void fx_readRegisterSpace()
	{
		int i;
		int position = 0;

		vErrorCode = 0;

		// Update R0-R15		
		for( i = 0; i < 16; i++)
		{
			avReg[i] = pvRegisters.get8Bit(position) & 0xFF;
			position++;
			avReg[i] += pvRegisters.get8Bit(position) << 8;
			position++;
		}

		// Update other registers
		vStatusReg = pvRegisters.get8Bit(GSU_SFR);
		vStatusReg |= pvRegisters.get8Bit( GSU_SFR + 1) << 8;
		vPrgBankReg = pvRegisters.get8Bit( GSU_PBR );
		vRomBankReg = pvRegisters.get8Bit( GSU_ROMBR );
		vRamBankReg = pvRegisters.get8Bit( GSU_RAMBR ) & ( FX_RAM_BANKS - 1 );
		vCacheBaseReg = pvRegisters.get8Bit( GSU_CBR );
		vCacheBaseReg |= pvRegisters.get8Bit( GSU_CBR + 1 ) << 8;

		// Update status register variables
		vZero = (vStatusReg & FLG_Z) != FLG_Z ? 1 : 0;
		vSign = (vStatusReg & FLG_S) << 12;
		vOverflow = (vStatusReg & FLG_OV) << 16;
		vCarry = (vStatusReg & FLG_CY) >> 2;

		// Set bank pointers
		pvRamBank = apvRamBank[vRamBankReg & 0x3];
		pvRomBank = apvRomBank[vRomBankReg];
		pvPrgBank = apvRomBank[vPrgBankReg];

		// Set screen pointers
		pvScreenBase = pvRam.getOffsetBuffer( USEX8( pvRegisters.get8Bit( GSU_SCBR) ) << 10 );
		
		i = ( pvRegisters.get8Bit(GSU_SCMR) & 0x04) != 0 ? 1 : 0;
		
		i |= ( ( pvRegisters.get8Bit(GSU_SCMR) & 0x20) != 0 ? 1 : 0 ) << 1;
		
		vScreenHeight = vScreenRealHeight = avHeight[i];
		vMode = pvRegisters.get8Bit(GSU_SCMR) & 0x03;
	
		if(i == 3)
			vScreenSize = (256/8) * (256/8) * 32;
		else
			vScreenSize = (vScreenHeight / 8) * (256 / 8) * avMult[vMode];
		
		if ( ( vPlotOptionReg & 0x10 ) != 0 )
		{
			// OBJ Mode (for drawing into sprites)
			vScreenHeight = 256;
		}

		// NAC: Double check this
		if( pvScreenBase.getOffset() + vScreenSize > (nRamBanks * 65536) )
			pvScreenBase =pvRam.getOffsetBuffer( (nRamBanks * 65536) - vScreenSize );

		fx_computeScreenPointers ();

		fx_backupCache();
	}

	void fx_dirtySCBR()
	{
		vSCBRDirty = true;
	}

	private void fx_computeScreenPointers ()
	{
		if (vMode != vPrevMode || vPrevScreenHeight != vScreenHeight ||vSCBRDirty)
		{
			int i;

			vSCBRDirty = false;

			// Make a list of pointers to the start of each screen column
			switch (vScreenHeight)
			{
				case 128:
				switch (vMode)
				{
					case 0:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer( i << 4 );
						x[i] = i << 8;
					}
					break;
					case 1:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 5);
						x[i] = i << 9;
					}
					break;
					case 2:
					case 3:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 6);
						x[i] = i << 10;
					}
					break;
				}
				break;
				case 160:
				switch (vMode)
				{
					case 0:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 4);
						x[i] = (i << 8) + (i << 6);
					}
					break;
					case 1:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 5);
						x[i] = (i << 9) + (i << 7);
					}
					break;
					case 2:
					case 3:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 6);
						x[i] = (i << 10) + (i << 8);
					}
					break;
				}
				break;
				case 192:
				switch (vMode)
				{
					case 0:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 4);
						x[i] = (i << 8) + (i << 7);
					}
					break;
					case 1:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 5);
						x[i] = (i << 9) + (i << 8);
					}
					break;
					case 2:
					case 3:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer(i << 6);
						x[i] = (i << 10) + (i << 9);
					}
					break;
				}
				break;
				case 256:
				switch (vMode)
				{
					case 0:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer( ( (i & 0x10) << 9) + ( (i & 0xf) << 8 ) );
						x[i] = ((i & 0x10) << 8) + ((i & 0xf) << 4);
					}
					break;
					case 1:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer( ( (i & 0x10) << 10) + ( (i & 0xf) << 9) );
						x[i] = ((i & 0x10) << 9) + ((i & 0xf) << 5);
					}
					break;
					case 2:
					case 3:
					for (i = 0; i < 32; i++)
					{
						apvScreen[i] = pvScreenBase.getOffsetBuffer( ( (i & 0x10) << 11) + ( (i & 0xf) << 10) );
						x[i] = ((i & 0x10) << 10) + ((i & 0xf) << 6);
					}
					break;
				}
				break;
			}
			vPrevMode = vMode;
			vPrevScreenHeight = vScreenHeight;
		}
	}

	private void fx_writeRegisterSpace()
	{
		int i;
		int position = 0;

		for( i = 0; i < 16; i++)
		{
			avReg[i] = pvRegisters.get8Bit(position) & 0xFF;
			position++;
			avReg[i] += pvRegisters.get8Bit(position) << 8;
			position++;
		}

		// Update status register
		if( USEX16(vZero) == 0 )
		{
			vStatusReg |= FLG_Z;
		} else {
			vStatusReg &= ~FLG_Z;
		}
		
		if( ( vSign & 0x8000 ) != 0 )
		{
			vStatusReg |= FLG_S;
		} else {
			vStatusReg &= ~FLG_S;
		}
		if(vOverflow >= 0x8000 || vOverflow < -0x8000)
		{
			vStatusReg |= FLG_OV;
		} else {
			vStatusReg &= ~FLG_OV;
		}
		
		if( vCarry != 0)
		{
			vStatusReg |= FLG_CY;
		} else {
			vStatusReg &= ~FLG_CY;
		}

		pvRegisters.put8Bit(GSU_SFR, vStatusReg);
		pvRegisters.put8Bit(GSU_SFR + 1, vStatusReg >> 8);
		pvRegisters.put8Bit(GSU_PBR, vPrgBankReg);
		pvRegisters.put8Bit(GSU_ROMBR, vRomBankReg);
		pvRegisters.put8Bit(GSU_RAMBR, vRomBankReg);
		pvRegisters.put8Bit(GSU_CBR, vCacheBaseReg);
		pvRegisters.put8Bit(GSU_CBR + 1, vCacheBaseReg >> 8);

		fx_restoreCache();
	}

	// Reset the FxChip 
	void FxReset()
	{
		int i;
		
		// NAC: Not sure if these are needed
		//static uint32 (**appfFunction[])(uint32) = { &fx_apfFunctionTable[0] };
		//static void (**appfPlot[])() = { &fx_apfPlotTable[0] };
		//static void (**appfOpcode[])() = { &fx_apfOpcodeTable[0] };
		
		
		// Get function pointers for the current emulation mode 
		//fx_ppfFunctionTable = appfFunction[psFxInfo->vFlags & 0x3];
		//fx_ppfPlotTable = appfPlot[psFxInfo->vFlags & 0x3];
		//fx_ppfOpcodeTable = appfOpcode[psFxInfo->vFlags & 0x3];

		// Clear all internal variables 
	    //memset((uint8*)&GSU,0,sizeof(struct FxRegs_s));
	    zero();

		// Set default registers 
		pvSreg = pvDreg = 0;

		// Set RAM and ROM pointers 
		vPrevScreenHeight = ~0;
		vPrevMode = ~0;

		// The GSU can't access more than 2mb (16mbits) 
		if(nRomBanks > 0x20)
			nRomBanks = 0x20;

		// Clear FxChip register space 		
		pvRegisters.fill(0, 0, 0x300);

		// Set FxChip version Number 
		pvRegisters.put8Bit(0x3b, 0);

		// Make ROM bank table
		for(i=0; i<256; i++)
		{
			int b = i & 0x7f;
			
			if (b >= 0x40)
			{
				if (nRomBanks > 1)
					b %= nRomBanks;
				else
					b &= 1;
	
				apvRomBank[i] = pvRom.getOffsetBuffer( b << 16 );
			}
			else
			{
				b %= nRomBanks * 2;
				apvRomBank[i] = pvRom.getOffsetBuffer( (b << 16) + 0x200000 );
			}
		}

		// Make RAM bank table 
		for(i=0; i<4; i++)
		{
			apvRamBank[i] = pvRam.getOffsetBuffer( (i % nRamBanks) << 16 );
			apvRomBank[0x70 + i] = apvRamBank[i];
		}

		// Start with a nop in the pipe
		vPipe = 0x01;

		// Set pointer to GSU cache 
		pvCache = pvRegisters.getOffsetBuffer(0x100);

		fx_readRegisterSpace();
		
	}

	private boolean fx_checkStartAddress()
	{
		// Check if we start inside the cache
		if( bCacheActive != 0 && avReg[15] >= vCacheBaseReg && ( avReg[15] < (vCacheBaseReg+512) ) )
			return true;

		//Check if we're in an unused area

		if( vPrgBankReg >= 0x60 && vPrgBankReg <= 0x6f)
			return false;
		
		if( vPrgBankReg >= 0x74)
			return false;

		// Check if we're in RAM and the RAN flag is not set
		if( vPrgBankReg >= 0x70 && vPrgBankReg <= 0x73 && ( USEX8(pvRegisters.get8Bit(GSU_SCMR)) & ( 1 <<3 ) ) == 0)
			return false;

		if( ( USEX8(pvRegisters.get8Bit(GSU_SCMR) ) & ( 1 << 4 ) ) == 0 )
			return false;

		return true;
	}

	// Execute until the next stop instruction 
	int FxEmulate(int nInstructions)
	{
		int vCount = 0;

		// Read registers and initialize GSU session 
		fx_readRegisterSpace();

		// Check if the start address is valid 
		if(!fx_checkStartAddress())
		{
			vStatusReg &= ~FLG_G;
			fx_writeRegisterSpace();
			return 0;
		}

		// Execute GSU session
		vStatusReg &= ~FLG_IRQ;

		if(bBreakPoint != 0)
		{
			vCount = fx_run_to_breakpoint(nInstructions);
		} else {
			vCount = fx_step_over(nInstructions);
		}

		// Store GSU registers 
		fx_writeRegisterSpace();

		// Check for error code
		if(vErrorCode != 0)
			return vErrorCode;
		else
			return vCount;
	}

	// Breakpoints
	private void FxBreakPointSet(int vAddress)
	{
		bBreakPoint = 1;
		vBreakPoint = USEX16(vAddress);
	}
	private void FxBreakPointClear()
	{
		bBreakPoint = 0;
	}

	// Step by step execution
	int FxStepOver(int nInstructions)
	{
		int vCount = 0;
		fx_readRegisterSpace();

		// Check if the start address is valid
		if( ! fx_checkStartAddress() )
		{
			vStatusReg &= ~FLG_G;
			return 0;
		}

		if( vPipe >= 0xf0 )
		{
			vStepPoint = USEX16(avReg[15]+3);
		}
		else if( (vPipe >= 0x05 && vPipe <= 0x0f) || (vPipe >= 0xa0 && vPipe <= 0xaf) )
		{
			vStepPoint = USEX16(avReg[15]+2);
		}
		else
		{
			vStepPoint = USEX16(avReg[15]+1);
		}
		
		vCount = fx_step_over(nInstructions);
		
		fx_writeRegisterSpace();
		
		if(vErrorCode != 0)
			return vErrorCode;
		else
			return vCount;
	}
	
	private void FxPipeString(byte pvString[])
	{

	    int vOpcode = (vStatusReg & 0x300) | (vPipe);
	    //const char *m = fx_apvMnemonicTable[vOpcode];
	    int vPipe1,vPipe2,vByte1,vByte2;
	    int vPipeBank = vPipeAdr >> 16;

	    // The next two bytes after the pipe's address
	    vPipe1 = apvRomBank[vPipeBank].get8Bit(USEX16(vPipeAdr+1));
	    vPipe2 = apvRomBank[vPipeBank].get8Bit(USEX16(vPipeAdr+2));

	    // The actual next two bytes to be read
	    vByte1 = pvPrgBank.get8Bit(USEX16(avReg[15]));
	    vByte2 = pvPrgBank.get8Bit(USEX16(avReg[15]+1));

	    // Print ROM address of the pipe
	    //sprintf(pvString, "%02x:%04x %02x ", USEX8(vPipeBank), USEX16(vPipeAdr), USEX8(vPipe));
	    //p = &pvString[strlen(pvString)];

	    // Check if it's a branch instruction
	    if( vPipe >= 0x05 && vPipe <= 0x0f )
	    {
			//sprintf(&pvString[11], "%02x    ", USEX8(vPipe1));
			//sprintf(p, m, USEX16(R15 + SEX8(vByte1) + 1 ) );
	    }
	    // Check for 'move' instruction
	    else if( vPipe >= 0x10 && vPipe <= 0x1f && ( vStatusReg & FLG_B ) == FLG_B )
	    {
	    	//sprintf(p, "move r%d,r%d", USEX8(vPipe & 0x0f), (uint32)(pvSreg - avReg));
	    }
	    // Check for 'ibt', 'lms' or 'sms'
	    else if( vPipe >= 0xa0 && vPipe <= 0xaf )
	    {
	    	//sprintf(&pvString[11], "%02x    ", USEX8(vPipe1));	    
			if( (vStatusReg & 0x300) == 0x100 || (vStatusReg & 0x300) == 0x200 )
			{
			    //sprintf(p, m, USEX16(vByte1) << 1 );
			}
			else
			{
			    //sprintf(p, m, USEX16(vByte1) );
		    }
	    }
	    // Check for 'moves'
	    else if( vPipe >= 0xb0 && vPipe <= 0xbf && ( vStatusReg & FLG_B ) == FLG_B )
	    {
	    	// sprintf(p, "moves r%d,r%d", (uint32)(pvDreg - avReg), USEX8(vPipe & 0x0f));
	    	// Check for 'iwt', 'lm' or 'sm'
	    }
	    else if( vPipe >= 0xf0 )
	    {
	    	//sprintf(&pvString[11], "%02x %02x ", USEX8(vPipe1), USEX8(vPipe2));
	    	//sprintf(p, m, USEX8(vByte1) | (USEX16(vByte2)<<8) );
	    }
	    // Normal instruction
	    else
	    {
	    	//strcpy(p, m);
	    }
	}

	// Errors 
	private int FxGetErrorCode()
	{
		return vErrorCode;
	}

	private int FxGetIllegalAddress()
	{
		return vIllegalAddress;
	}

	// Access to internal registers 
	private int FxGetColorRegister()
	{
		return vColorReg & 0xff;
	}

	private int FxGetPlotOptionRegister()
	{
		return vPlotOptionReg & 0x1f;
	}

	private int FxGetSourceRegisterIndex()
	{
		return pvSreg;
	}

	private int FxGetDestinationRegisterIndex()
	{
		return pvDreg;
	}

	private int FxPipe()
	{
		return vPipe;
	}
	
	private void fx_plot()
	{
		switch(vMode)
		{
			case 0x0: fx_plot_2bit(); break;	
			case 0x1: fx_plot_4bit(); break;
			case 0x2: fx_plot_4bit(); break;
			case 0x3: fx_plot_8bit(); break;
			case 0x4: fx_plot_obj(); break;
		}
	}
	
	private void fx_rpix()
	{
		switch(vMode)
		{
			case 0x0: fx_rpix_2bit(); break;	
			case 0x1: fx_rpix_4bit(); break;
			case 0x2: fx_rpix_4bit(); break;
			case 0x3: fx_rpix_8bit(); break;
			case 0x4: fx_rpix_obj(); break;
		}
	}
	
	private void zero()
	{
		for ( int i = 0; i < avReg.length; i++)
			avReg[i] = 0;
		
		vColorReg = 0;
		vPlotOptionReg = 0;
		vStatusReg = 0;
		vPrgBankReg = 0;
		vRomBankReg = 0;
		vRamBankReg = 0;
		vCacheBaseReg = 0;
		vCacheFlags = 0;
		vLastRamAdr = 0;
		pvDreg = 0;
		pvSreg = 0;
		vRomBuffer = 0;
		vPipe = 0;
		vPipeAdr = 0;

		vSign = 0;
		vZero = 0;
		vCarry = 0;
		vOverflow = 0;

		vErrorCode = 0;
		vIllegalAddress = 0;

		bBreakPoint = 0;
		vBreakPoint = 0;
		vStepPoint = 0;

		nRamBanks = 0;
		nRomBanks = 0;

		vMode = 0;
		vPrevMode = 0;
		
		vScreenHeight = 0;
		vScreenRealHeight = 0;
		vPrevScreenHeight = 0;
		vScreenSize = 0;

		bCacheActive = 0;
		vCounter = 0;
		vInstCount = 0;
		vSCBRDirty = false;	
	}
	
}
