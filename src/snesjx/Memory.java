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

import java.io.*;
import java.util.Arrays;

class Memory
{
	// Block size of 4096
	static final int MEMMAP_BLOCK_SIZE = 0x1000;
	// 4096 Blocks, Each block is 4096 bytes in size
	static final int MEMMAP_NUM_BLOCKS = 0x1000000 / MEMMAP_BLOCK_SIZE;
	static final int MEMMAP_MASK = MEMMAP_BLOCK_SIZE - 1;
	
	// Max ROM Size is 8 MB exactly
	static final int MAX_ROM_SIZE = 0x800000;
	
	static final int FillRamOffset = 0x40000; 
	
	private static final int ROM_NAME_LEN = 23;
	
	private static final int NOPE = 0;
	private static final int YEAH = 1;
	private static final int BIGFIRST = 2;
	private static final int SMALLFIRST = 3;
	
	private static final int MAP_TYPE_I_O  = 0;
	private static final int MAP_TYPE_ROM  = 1;
	private static final int MAP_TYPE_RAM  = 2;
	
	static final int MAP_PPU		   = 0x6FFFFFFF;
	static final int MAP_CPU		   = 0x6FFFFFFE;
	static final int MAP_LOROM_SRAM	= 0x6FFFFFFD;
	static final int MAP_LOROM_SRAM_B  = 0x6FFFFFFC;
	static final int MAP_HIROM_SRAM	= 0x6FFFFFFB;
	static final int MAP_DSP		   = 0x6FFFFFFA;
	static final int MAP_C4			= 0x6FFFFFF9;
	static final int MAP_BWRAM		 = 0x6FFFFFF8;
	static final int MAP_BWRAM_BITMAP  = 0x6FFFFFF7;
	static final int MAP_BWRAM_BITMAP2 = 0x6FFFFFF6;
	static final int MAP_SA1RAM		= 0x6FFFFFF5;
	static final int MAP_SPC7110_ROM   = 0x6FFFFFF4;
	static final int MAP_SPC7110_DRAM  = 0x6FFFFFF3;
	static final int MAP_RONLY_SRAM	= 0x6FFFFFF2;
	static final int MAP_OBC_RAM	   = 0x6FFFFFF1;
	static final int MAP_SETA_DSP	  = 0x6FFFFFF0;
	static final int MAP_SETA_RISC	 = 0x6FFFFFEF;
	static final int MAP_BSX		   = 0x6FFFFFEE;
	static final int MAP_NONE		  = 0x6FFFFFED;
	static final int MAP_DEBUG		 = 0x6FFFFFEC;
	static final int MAP_LAST		  = 0x6FFFFFEB;
	
	static final int MEMMAP_SHIFT = 12;

	static final int WRAP_NONE = 0;
	static final int WRAP_BANK = 1;
	static final int WRAP_PAGE = 2;
	
	static final int WRITE_01 = 0;
	static final int WRITE_10 = 1;
	
	ByteArray NSRTHeader = new ByteArray(32);
	
	ByteArray RAM;
	ByteArrayOffset ROM;
	ByteArrayOffset SRAM;
	ByteArray VRAM;
	ByteArray FillRAM;
	ByteArrayOffset BWRAM;
	//ByteArrayOffset C4RAM;
	//ByteArrayOffset BSRAM;

	int[] Map = new int[MEMMAP_NUM_BLOCKS];
	int[] WriteMap = new int[MEMMAP_NUM_BLOCKS];
	
	boolean[] BlockIsRAM = new boolean[MEMMAP_NUM_BLOCKS];
	boolean[] BlockIsROM = new boolean[MEMMAP_NUM_BLOCKS];
	int[] MemorySpeed = new int[MEMMAP_NUM_BLOCKS];
	
	private short ExtendedFormat;
	
	private String ROMFilename;

	private ByteArray ROMName;
	
	private ByteArray ROMId;
	private String CompanyId;
	
	private int ROMRegion;
	private int ROMSpeed;
	private int ROMType;
	private int ROMSize;
	
	private int ROMChecksum;
	private int ROMComplementChecksum;
	private int ROMCRC32;
	int ROMFramesPerSecond;

	private boolean HiROM;
	private boolean LoROM;
	int SRAMSize;
	int SRAMMask;
	int CalculatedSize;
	private long CalculatedChecksum;
	
	private static String LastRomFilename = "";

	private static final int[] crc32Table = GenerateCrcTable();

	private boolean isChecksumOK = false;
	
	private short SRAMInitialValue;

	private Globals globals;
	private PPU ppu;
	private Settings settings;
	private SuperFX superfx;
	private DSP1 dsp1;
	private SA1 sa1;
	private SMulti Multi;
	private Timings timings;
	private CPU cpu;

	void setUp()
	{
		globals = Globals.globals;
		ppu = globals.ppu;
		settings = globals.settings;
		superfx = globals.superfx;
		dsp1 = globals.dsp1;
		sa1 = globals.sa1;
		Multi = globals.Multi;
		timings = globals.timings;
		cpu = globals.cpu;
	}
	
	void SetupMemory(int RomByteSize)
	{
		// VRAM 64k
		VRAM = new ByteArray(0x10000);
		
		// RAM (128K) + SRAM (128K) + FILLRAM (32K) + ROM + Lil Extra
		RAM  = new ByteArray(0x20000 + 0x20000 + 0x8000 + RomByteSize + 0x200 );
		SRAM = RAM.getOffsetBuffer(0x20000);
		//FillRAM = RAM.getOffsetBuffer(0x40000);
		FillRAM = new ByteArray(0x8000);

		ppu.TileCached[PPU.TILE_2BIT] = new byte[PPU.MAX_2BIT_TILES * 64];
		ppu.TileCached[PPU.TILE_4BIT] = new byte[PPU.MAX_4BIT_TILES * 64];
		ppu.TileCached[PPU.TILE_8BIT] = new byte[PPU.MAX_8BIT_TILES * 64];

		ppu.ZeroTileCache();

		// FillRAM uses first 32K of ROM image area, otherwise space just
		// wasted. Might be read by the SuperFX code.

		//FillRAM = ROM;

		// Add 0x8000 to ROM image pointer to stop SuperFX code accessing
		// unallocated memory (can cause crash on some ports).
		ROM = RAM.getOffsetBuffer( 0x48000 );

		if ( SnesSystem.DEBUG_MEM )
		{
			System.out.println( "ROM offset: " + ROM.getOffset() );	
		}
		
		//NAC: Not used right now
		//C4RAM = ROM.getOffsetBuffer( 0x400000 + 8192 * 8 );
		//BSRAM = ROM.getOffsetBuffer( 0x400000 );
		
		superfx.pvRegisters = FillRAM.getOffsetBuffer(0x3000);
		superfx.nRamBanks = 2;
		superfx.pvRam = SRAM;
		superfx.nRomBanks = (2 * 1024 * 1024) / (32 * 1024);
		superfx.pvRom = ROM;
	}

	private static int[] GenerateCrcTable()
	{
		int[] crc_table = new int[256];
		
		for (int n = 0; n < 256; n++)
		{
			int c = n;
			for (int k = 8; --k >= 0; )
			{
				if ((c & 1) != 0)
				{
					c = 0xedb88320 ^ (c >>> 1);
				}
				else
				{
					c = c >>> 1;
				}
			}
			crc_table[n] = c;
		}
		return crc_table;
	}
	
	private static void DeinterleaveType1 (int size, ByteArrayOffset base)
	{
		ByteArray blocks = new ByteArray(256);
		
		int nblocks = size >>> 16;

		for (int i = 0; i < nblocks; i++)
		{
			blocks.put8Bit(i * 2, (i + nblocks));
			blocks.put8Bit(i * 2 + 1, i);
		}

		ByteArray tmp = new ByteArray(0x8000);
		
		for (int i = 0; i < nblocks * 2; i++)
		{
			for (int j = i; j < nblocks * 2; j++)
			{
				if (blocks.get8Bit(j) == i)
				{
					// Copy base to temp, base to base and finally temp back to base
					tmp.arraycopy(0, base, blocks.get8Bit(j) * 0x8000, 0x8000 );
					base.arraycopy(blocks.get8Bit(j) * 0x8000, base, blocks.get8Bit(i) * 0x8000, 0x8000);
					base.arraycopy(blocks.get8Bit(i) * 0x8000, tmp, 0, 0x8000);
										
					int b = blocks.get8Bit(j);
					blocks.put8Bit(j, blocks.get8Bit(i));
					blocks.put8Bit(i, b);
					break;
				}
			}
		}
	}
   
	private static void DeinterleaveType2 (int size, ByteArrayOffset base)
	{
		ByteArray blocks = new ByteArray(256);
		int nblocks = size >>> 16;
		int	step = 64;

		while (nblocks <= step)
			step >>>= 1;
		nblocks = step;

		for (int i = 0; i < nblocks * 2; i++)
			blocks.put8Bit(i, ((i & ~0xf) | ((i & 3) << 2) | ((i & 12) >>> 2)));

		ByteArray tmp = new ByteArray(0x10000);
		
		for (int i = 0; i < nblocks * 2; i++)
		{
			for (int j = i; j < nblocks * 2; j++)
			{
				if (blocks.get8Bit(j) == i)
				{
					// Copy base to temp, base to base and finally temp back to base
					tmp.arraycopy(0, base, blocks.get8Bit(j) * 0x1000, 0x1000 );
					base.arraycopy(blocks.get8Bit(j) * 0x1000, base, blocks.get8Bit(i) * 0x1000, 0x1000);
					base.arraycopy(blocks.get8Bit(i) * 0x1000, tmp, 0, 0x1000);

					int b = blocks.get8Bit(j);
					blocks.put8Bit(j, blocks.get8Bit(i));
					blocks.put8Bit(i, b);

					break;
				}
			}
		}

	}	

	private static void DeinterleaveGD24 (int size, ByteArrayOffset base)
	{
		// for 24Mb images dumped with Game Doctor
		if (size != 0x300000)
			return;

		ByteArray tmp = new ByteArray(0x80000);
	
		tmp.arraycopy(0, base, 0x180000, 0x80000);
		base.arraycopy(0x180000, base, 0x200000, 0x80000);
		base.arraycopy(0x200000, base, 0x280000, 0x80000);
		base.arraycopy(0x280000, tmp, 0, 0x80000);

		DeinterleaveType1(size, base);
	}
   
	// Remove the NSRT Header from the ROM if it exists
	private int HeaderRemove(int size, ByteArrayOffset buf)
	{
		int calc_size = (size / 0x2000) * 0x2000;
   
		if (size - calc_size == 512 )
		{

			ByteArrayOffset NSRTHead = buf.getOffsetBuffer(0x1D0);
		
			// Does "NSRT" exist at offset 24
			if ( NSRTHead.compare(24, "NSRT".getBytes() ) && NSRTHead.get8Bit(28) == 22 )
			{
				// If the sum of the entire NSRTHead masked off 0xFF equals position 30
				if (
						( Accumulate( NSRTHead, 32 ) & 0xFF ) == NSRTHead.get8Bit(30) &&
						( NSRTHead.get8Bit(30) + NSRTHead.get8Bit(31) == 255 ) &&
						( ( NSRTHead.getByte(0) & 0x0F ) <= 13 ) &&
						( ( ( NSRTHead.getByte(0) & 0xF0 ) >>> 4 ) <= 3 ) &&
						( ( ( NSRTHead.getByte(0) & 0xF0 ) >>> 4 ) > 0 )
						
				)
				{
					NSRTHeader.arraycopy(0, NSRTHead, 0, 32 );
				}
			}
			
			buf.arraycopy(0, buf, 512, calc_size);
			size -= 512;
		}
		
		return size;
	}
  
	// Sum the values in the range specified
	private final int Accumulate( ByteArrayOffset buf, int size ) 
	{
		int result = 0;

		for( int i = 0 ; i < size ; i++ ) {
			result += buf.get8Bit(i);
		}
		return result;
	}

	private int ScoreHiROM( boolean skip_header )
	{
		return ScoreHiROM( skip_header, 0);
	}
	
	private int ScoreHiROM( boolean skip_header, int romoff)
	{
		ByteArrayOffset buf = ROM.getOffsetBuffer( 0xff00 + romoff + (skip_header ? 0x200 : 0) );
		
		int	score = 0;

		if ((buf.getByte(0xd5) & 0x1) > 0)
			score += 2;

		// Mode23 is SA-1
		if (buf.get8Bit(0xd5) == 0x23)
			score -= 2;

		if (buf.get8Bit(0xd4) == 0x20)
			score += 2;

		if ((buf.get8Bit(0xdc) + (buf.get8Bit(0xdd) << 8)) + (buf.get8Bit(0xde) + (buf.get8Bit(0xdf) << 8)) == 0xffff)
		{
			score += 2;
			if (0 != (buf.get8Bit(0xde) + (buf.get8Bit(0xdf) << 8)))
				score++;
		}

		if (buf.get8Bit(0xda) == 0x33)
			score += 2;

		if ((buf.getByte(0xd5) & 0xf) < 4)
			score += 2;

		if ( ! ((buf.getByte(0xfd) & 0x80) > 0) )
			score -= 6;

		if ((buf.get8Bit(0xfc) + (buf.get8Bit(0xfd) << 8)) > 0xffb0)
			score -= 2; // reduced after looking at a scan by Cowering

		if (CalculatedSize > 1024 * 1024 * 3)
			score += 4;

		if ((1 << (buf.get8Bit(0xd7) - 7)) > 48)
			score -= 1;

		if (!allASCII(buf.getOffsetBuffer(0xb0), 6))
			score -= 1;

		if (!allASCII(buf.getOffsetBuffer(0xc0), ROM_NAME_LEN - 1))
			score -= 1;
		
		return score;
	}

	private int ScoreLoROM( boolean skip_header )
	{
		return ScoreLoROM( skip_header, 0);
	}
	
	private int ScoreLoROM (boolean skip_header, int romoff)
	{
		ByteArrayOffset buf = ROM.getOffsetBuffer( 0x7f00 + romoff + (skip_header ? 0x200 : 0) );
		int		score = 0;

		if (! ( ( buf.getByte(0xd5) & 0x1 ) > 0 ) )
			score += 3;

		// Mode23 is SA-1
		if (buf.get8Bit(0xd5) == 0x23)
			score += 2;

		if ((buf.get8Bit(0xdc) + (buf.get8Bit(0xdd) << 8)) + (buf.get8Bit(0xde) + (buf.get8Bit(0xdf) << 8)) == 0xffff)
		{
			score += 2;
			if (0 != (buf.get8Bit(0xde) + (buf.get8Bit(0xdf) << 8)))
				score++;
		}

		if (buf.get8Bit(0xda) == 0x33)
			score += 2;

		if ((buf.getByte(0xd5) & 0xf) < 4)
			score += 2;

		if (! ((buf.getByte(0xfd) & 0x80) > 0 ) )
			score -= 6;

		if ((buf.get8Bit(0xfc) + (buf.get8Bit(0xfd) << 8)) > 0xffb0)
			score -= 2; // reduced per Cowering suggestion

		if (CalculatedSize <= 1024 * 1024 * 16)
			score += 2;

		if ((1 << (buf.get8Bit(0xd7) - 7)) > 48)
			score -= 1;


		if (!allASCII(buf.getOffsetBuffer(0xb0), 6))
			score -= 1;

		if (!allASCII(buf.getOffsetBuffer(0xc0), ROM_NAME_LEN - 1))
			score -= 1;

		return score;
	} 

	private String GetFileExtension( String fileName )
	{
		return fileName.toString().substring(fileName.lastIndexOf('.') + 1, fileName.length());
	}
	
	void LoadROM(String filename) throws Exception
	{
		if ( filename.length() == 0 )
			throw new Exception("Can not load '" + filename + "'" );
			
		// Zero out all 8MB of space
		//ROM.zero(); DJW, Already done once
		
		globals.Multi.zero();

		CalculatedSize = 0;
		ExtendedFormat = Memory.NOPE;

		int totalFileSize;
		
		File file = new File( filename );
		// Does the file exist?
		if( ! file.exists() || ! file.canRead() ) {
			throw new Exception( "Can not open '" + filename + "'");
		}
		
		int filesize = (int) file.length();

		// Is the file to large to load?
		if( filesize > MAX_ROM_SIZE ) {
			throw new Exception( "'" + filename + "' is to large to load, Multi-File ROMS are not supported" );
		}
		
		SetupMemory( filesize );

		// Get the file extension
		String fileNameExt = GetFileExtension( filename );
		
		// If we were passed a zip file
		if( fileNameExt == "zip" ) 
		{
			throw new Exception( "Zip Archives not supported" );
		}	

		FileInputStream input = new FileInputStream( filename );
		// Read in the entire file
		
		totalFileSize = input.read( ROM.buffer, ROM.getOffset() , filesize  );
		// close the file
		input.close();

		// Did we read the entire file?
		if( totalFileSize != file.length() ) {
			throw new Exception( "Read '" + totalFileSize + "' bytes, but file size was '" + file.length() + "' from '" + filename + "'" );
		}

		// Remove the NSRT header if it exists
		totalFileSize = HeaderRemove( totalFileSize, ROM );

		// Set our ROM filename
		ROMFilename = filename;
		
		if( totalFileSize == 0 ) 
			throw new Exception("Can not load a ROM of zero size '" + filename + "'" );

		int	hi_score, lo_score;
		
		hi_score = ScoreHiROM(false);
		lo_score = ScoreLoROM(false);

		CalculatedSize = (totalFileSize / 0x2000) * 0x2000;

		// If the size of the ROM is over 4MB
		if (CalculatedSize > 0x400000 &&
			(ROM.get8Bit(0x7fd5) + (ROM.get8Bit(0x7fd6) << 8)) != 0x4332 && // exclude S-DD1
			(ROM.get8Bit(0x7fd5) + (ROM.get8Bit(0x7fd6) << 8)) != 0x4532 &&
			(ROM.get8Bit(0xffd5) + (ROM.get8Bit(0xffd6) << 8)) != 0xF93a && // exclude SPC7110
			(ROM.get8Bit(0xffd5) + (ROM.get8Bit(0xffd6) << 8)) != 0xF53a)
		{
			ExtendedFormat = YEAH;
		}

		// if both vectors are invalid, it's type 1 LoROM
		if (ExtendedFormat == NOPE &&
			((ROM.get8Bit(0x7ffc) + (ROM.get8Bit(0x7ffd) << 8)) < 0x8000) &&
			((ROM.get8Bit(0xfffc) + (ROM.get8Bit(0xfffd) << 8)) < 0x8000))
		{
				DeinterleaveType1(totalFileSize, ROM);
		}

		// CalculatedSize is now set, so re-score
		ByteArrayOffset RomHeader = ROM.getOffsetBuffer( 0 );

		if (ExtendedFormat != NOPE)
		{
			int	swappedhirom, swappedlorom;

			swappedhirom = ScoreHiROM(false, 0x400000);
			swappedlorom = ScoreLoROM(false, 0x400000);

			// set swapped here
			if (Math.max(swappedlorom, swappedhirom) >= Math.max(lo_score, hi_score))
			{
				ExtendedFormat = BIGFIRST;
				hi_score = swappedhirom;
				lo_score = swappedlorom;
				RomHeader = RomHeader.getOffsetBuffer(0x400000);
			}
			else
				ExtendedFormat = SMALLFIRST;
		}

		boolean interleaved, tales = false;

		interleaved = false;

		if ( lo_score >= hi_score )
		{
			LoROM = true;
			HiROM = false;

			// ignore map type byte if not 0x2x or 0x3x
			if ((RomHeader.getByte(0x7fd5) & 0xf0) == 0x20 || (RomHeader.getByte(0x7fd5) & 0xf0) == 0x30)
			{
				switch (RomHeader.getByte(0x7fd5) & 0xf)
				{
					case 1:
						interleaved = true;
						break;

					case 5:
						interleaved = true;
						tales = true;
						break;
				}
			}
		}
		else
		{
			LoROM = false;
			HiROM = true;

			if ((RomHeader.get8Bit(0xffd5) & 0xf0) == 0x20 || (RomHeader.get8Bit(0xffd5) & 0xf0) == 0x30)
			{
				switch (RomHeader.get8Bit(0xffd5) & 0xf)
				{
					case 0:
					case 3:
						interleaved = true;
						break;
				}
			}
		}

		// this two games fail to be detected
		if (	ROM.compare(0x7fc0, "YUYU NO QUIZ DE GO!GO!".getBytes() ) ||
				ROM.compare(0xffc0, "BATMAN--REVENGE JOKER".getBytes() )
		)
		{
			LoROM = true;
			HiROM = false;
			interleaved = false;
			tales = false;
		}

		if ( interleaved )
		{
			SnesSystem.Message(SnesSystem._INFO, SnesSystem._ROM_INTERLEAVED_INFO, "ROM image is in interleaved format - converting...");

			if (tales)
			{
				if (ExtendedFormat == BIGFIRST)
				{
					DeinterleaveType1(0x400000, ROM.getOffsetBuffer(0x400000));
					DeinterleaveType1(CalculatedSize - 0x400000, ROM);
				}
				else
				{
					DeinterleaveType1(CalculatedSize - 0x400000, ROM);
					DeinterleaveType1(0x400000, ROM.getOffsetBuffer(CalculatedSize - 0x400000));
				}

				LoROM = false;
				HiROM = true;
			}
			else
			{
				boolean	t = LoROM;
				LoROM = HiROM;
				HiROM = t;
				DeinterleaveType1(CalculatedSize, ROM);
			}

			hi_score = ScoreHiROM(false);
			lo_score = ScoreLoROM(false);

			if ((HiROM && (lo_score >= hi_score || hi_score < 0)) ||
				(LoROM && (hi_score >  lo_score || lo_score < 0)))
			{
				SnesSystem.Message(SnesSystem._INFO, SnesSystem._ROM_CONFUSING_FORMAT_INFO, "ROM lied about its type! Trying again.");
				throw new Exception( "ROM lies about its type!");
			}
		}

		if (ExtendedFormat == SMALLFIRST)
			tales = true;


		if (tales)
		{
			ByteArray tmp = new ByteArray(CalculatedSize - 0x400000);
			SnesSystem.Message(SnesSystem._INFO, SnesSystem._ROM_INTERLEAVED_INFO, "Fixing swapped ExHiROM...");
			tmp.arraycopy(0, ROM, 0, CalculatedSize - 0x400000);
			ROM.arraycopy(0, ROM, CalculatedSize - 0x400000, 0x400000);
			ROM.arraycopy(0x400000, tmp, 0, CalculatedSize - 0x400000);
		}
		
		// TODO: FreeSDD1Data
		// FreeSDD1Data();
		
		//
		/* TODO: CleanUp7110
		if (CleanUp7110)
			(*CleanUp7110)();
		*/
		
		SRAMInitialValue = 0x60;
	}

	void ParseSNESHeader ( ByteArrayOffset RomHeader )
	{
		//System.out.println( "RomHeader: " + RomHeader.getOffset() );
		//System.out.println( "Value: " + RomHeader.getString(0x10, ROM_NAME_LEN) );
		ROMName = RomHeader.getRange(0x10, ROM_NAME_LEN );
		//System.out.println( "RomName: " + ROMName.getString() );
		ROMSize = RomHeader.get8Bit(0x27);

		SRAMSize  = RomHeader.get8Bit(0x28);
		ROMSpeed  = RomHeader.get8Bit(0x25);
		ROMType   = RomHeader.get8Bit(0x26);
		ROMRegion = RomHeader.get8Bit(0x29);

		ROMChecksum		   = RomHeader.get8Bit(0x2E) + ( RomHeader.get8Bit(0x2F) << 8 );
		ROMComplementChecksum = RomHeader.get8Bit(0x2C) + ( RomHeader.get8Bit(0x2D) << 8 );

		ROMId = RomHeader.getRange(0x02, 4);

		if (RomHeader.get8Bit(0x2A) == 0x33)
			CompanyId = RomHeader.getString(0x00, 2);
		else
			CompanyId = RomHeader.getHex(0x2A);
	}

	// DSP1/2/3/4
	private final void DecideWhichDSPToUse( ByteArrayOffset RomHeader ) throws Exception
	{
		settings.DSP1Master = false;

		dsp1.version = 0xff;

		if (ROMType == 0x03)
		{
			if (ROMSpeed == 0x30)
			{
				dsp1.version = 3; // DSP4
			}
			else
			{
				dsp1.version = 0; // DSP1
			}
		}
		else if (ROMType == 0x05)
		{
			if (ROMSpeed == 0x20)
			{
				dsp1.version = 1; // DSP2
			}
			else if (ROMSpeed == 0x30 && RomHeader.get8Bit(0x2a) == 0xb2)
			{
				dsp1.version = 2; // DSP3
			}
			else
			{
				dsp1.version = 0; // DSP1
			}
		}

		if (dsp1.version != 0xff)
			settings.DSP1Master = true;

		switch (dsp1.version)
		{
			case 0:	// DSP1
				if (HiROM)
				{
					dsp1.boundary = 0x7000;
					dsp1.maptype = DSP1.M_DSP1_HIROM;
				}
				else
				if (CalculatedSize > 0x100000)
				{
					dsp1.boundary = 0x4000;
					dsp1.maptype = DSP1.M_DSP1_LOROM_L;
				}
				else
				{
					dsp1.boundary = 0xc000;
					dsp1.maptype = DSP1.M_DSP1_LOROM_S;
				}

				// NAC: Doesn't appear to be utilized
				//globals.DSPInterface = DSP1.new DSP1ByteImpl();
				break;

			case 1: // DSP2
				throw new Exception( "DSP2 Not Implemented yet");
				//DSP1.boundary = 0x10000;
				//DSP1.maptype = SDSP1.M_DSP2_LOROM;
				//globals.DSPInterface = new SDSP1.DSP1ByteImpl();
				//break;

			case 2: // DSP3
				throw new Exception( "DSP3 Not Implemented yet");
				//DSP1.boundary = 0xc000;
				//DSP1.maptype = SDSP1.M_DSP3_LOROM;
				//globals.DSPInterface = new SDSP1.DSP1ByteImpl();
				//globals.DSPInterface.Reset();
				//break;

			case 3: // DSP4
				throw new Exception( "DSP4 Not Implemented yet");
				//DSP1.boundary = 0xc000;
				//DSP1.maptype = SDSP1.M_DSP4_LOROM;
				//globals.DSPInterface = new SDSP1.DSP1ByteImpl();
				//break;

			default:
				// TODO: DJW, Hopefully an un-initialized DSPInterface will not cause probs
				//SetDSP = NULL;
				//GetDSP = NULL;
				break;
		}
	}

	private final void DetectOptionalChips() throws Exception 
	{ 

		//settings.SuperFX = false;
		settings.SA1 = false;
		//settings.C4 = false;
		//settings.SDD1 = false;
		settings.SRTC = false;
		//settings.SPC7110 = false;
		//settings.SPC7110RTC = false;
		//settings.BS = false;
		//settings.OBC1 = false;
		//settings.SETA = false;

		//settings.SA1	 = settings.ForceSA1;
		//settings.SuperFX = settings.ForceSuperFX;
		//settings.SDD1	= settings.ForceSDD1;
		//settings.C4	  = settings.ForceC4;

		int	identifier = ((ROMType & 0xff) << 8) + (ROMSpeed & 0xff);

		switch (identifier)
		{
			// SRTC
			case 0x5535:
				settings.SRTC = true;
				break;

			// SPC7110
			case 0xF93A:
				//settings.SPC7110RTC = true;
				throw new Exception ( "No support for SPC7110 Chipset" );
			case 0xF53A:
				//settings.SPC7110 = true;
				throw new Exception ( "No support for SPC7110 Chipset" );
				//Spc7110Init();
				//break;

			// OBC1
			case 0x2530:
				//settings.OBC1 = true;
				throw new Exception ( "No support for OBC1 Chipset" );

			// SA1
			case 0x3423:
			case 0x3523:
				settings.SA1 = true;
				break;

			// SuperFX
			case 0x1320:
			case 0x1420:
			case 0x1520:
			case 0x1A20:
				throw new Exception ( "No support for SuperFX Chipset" );
				//settings.SuperFX = true;
				/*
				if (Settings.SuperFX)
				{
					if (ROM.get8Bit(0x7FDA) == 0x33)
						SRAMSize = ROM.get8Bit(0x7FBD);
					else
						SRAMSize = 5;
				}

				break;
				*/
			// SDD1
			case 0x4332:
			case 0x4532:
				throw new Exception ( "No support for SDD1 Chipset" );
				/*
				settings.SDD1 = !settings.ForceNoSDD1;
				if (settings.SDD1) {
					
					//LoadSDD1Data();
				}
				break;
				*/
			// ST018
			case 0xF530:
				throw new Exception ( "No support for ST018 Chipset" );
				/*
				Settings.SETA = ST_018;
				SetSETA = NULL;
				GetSETA = NULL;
				SRAMSize = 2;
				SNESGameFixes.SRAMInitialValue = 0x00;
				
				break;
				*/

			// ST010/011
			case 0xF630:
				throw new Exception ( "No support for ST010/011 Chipset" );
				/*
				if (ROM.get8Bit(0x7FD7) == 0x09)
				{
					Settings.SETA = ST_011;
					SetSETA = &SetST011;
					GetSETA = &GetST011;
				}
				else
				{
					Settings.SETA = ST_010;
					SetSETA = &SetST010;
					GetSETA = &GetST010;
				}

				SRAMSize = 2;
				SNESGameFixes.SRAMInitialValue = 0x00;
			   
				break;
				*/

			// C4
			case 0xF320:
				throw new Exception ( "No support for C4 Chipset" );
				//settings.C4 = !settings.ForceNoC4;
				//break;
		}
	}

	private final void Map_Initialize()
	{
		for (int c = 0; c < 0x1000; c++)
		{
			Map[c] = MAP_NONE;
			WriteMap[c] = MAP_NONE;
			
			BlockIsROM[c] = false;
			BlockIsRAM[c] = false;
		}
	}

	private final void map_space (int bank_s, int bank_e, int addr_s, int addr_e, int data)
	{
		int	c, i, p;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >>> 12);
				Map[p] = data;
				BlockIsROM[p] = false;
				BlockIsRAM[p] = true;
			}
		}
	}

	private final void map_index (int bank_s, int bank_e, int addr_s, int addr_e, int map_index, int type_rom_or_ram)
	{
		int	c, i, p;
		boolean isROM, isRAM;

		isROM = ((type_rom_or_ram == MAP_TYPE_I_O) || (type_rom_or_ram == MAP_TYPE_RAM)) ? false : true;
		isRAM = ((type_rom_or_ram == MAP_TYPE_I_O) || (type_rom_or_ram == MAP_TYPE_ROM)) ? false : true;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >>> 12);
				Map[p] = map_index;
				BlockIsROM[p] = isROM;
				BlockIsRAM[p] = isRAM;
			}
		}
	}

	private final void map_System ()
	{
		// will be overwritten
		map_space(0x00, 0x3f, 0x0000, 0x1fff, 0); // RAM was replaced with a the index of RAM
		map_index(0x00, 0x3f, 0x2000, 0x3fff, MAP_PPU, MAP_TYPE_I_O);
		map_index(0x00, 0x3f, 0x4000, 0x5fff, MAP_CPU, MAP_TYPE_I_O);
		map_space(0x80, 0xbf, 0x0000, 0x1fff, 0); // RAM was replaced with a the index of RAM
		map_index(0x80, 0xbf, 0x2000, 0x3fff, MAP_PPU, MAP_TYPE_I_O);
		map_index(0x80, 0xbf, 0x4000, 0x5fff, MAP_CPU, MAP_TYPE_I_O);
	}

	private final int map_mirror (int size, int pos)
	{
		if (size == 0) return 0;
		if (pos < size) return pos;

		int	mask = 1 << 31;
		while (!( ( pos & mask ) == mask) ) mask = mask >>> 1;

		if (size <= ( pos & mask ) ) return map_mirror(size, pos - mask);
		else return ( mask + map_mirror(size - mask, pos - mask) );
	}

	private final void map_lorom_offset (int bank_s, int bank_e, int addr_s, int addr_e, int size, int offset)
	{
		int	c, i, p, addr;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >> 12);
				addr = ((c - bank_s) & 0x7f) * 0x8000;
				Map[p] = ROM.getOffset() + offset + map_mirror(size, addr) - (i & 0x8000);
				BlockIsROM[p] = true;
				BlockIsRAM[p] = false;
			}
		}
	}

	private final void map_lorom (int bank_s, int bank_e, int addr_s, int addr_e, int size)
	{
		int	c, i, p, addr;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >>> 12);
				addr = (c & 0x7f) * 0x8000;
				
				// DJW: Map[p] = ROM + map_mirror(size, addr) - (i & 0x8000);
				Map[p] = ROM.getOffset() + map_mirror(size, addr) - (i & 0x8000);
				
				BlockIsROM[p] = true;
				BlockIsRAM[p] = false;
			}
		}
	}

	private final void map_hirom (int bank_s, int bank_e, int addr_s, int addr_e, int size)
	{
		int	c, i, p, addr;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >> 12);
				addr = c << 16;
				Map[p] = ROM.getOffset() + map_mirror(size, addr);
				BlockIsROM[p] = true;
				BlockIsRAM[p] = false;
			}
		}
	}

	private final void map_hirom_offset (int bank_s, int bank_e, int addr_s, int addr_e, int size, int offset)
	{
		int	c, i, p, addr;

		for (c = bank_s; c <= bank_e; c++)
		{
			for (i = addr_s; i <= addr_e; i += 0x1000)
			{
				p = (c << 4) | (i >>> 12);
				addr = (c - bank_s) << 16;

				// DJW: was Map[p] = ROM + offset + map_mirror(size, addr);
				Map[p] = ROM.getOffset() + offset + map_mirror(size, addr);
				BlockIsROM[p] = true;
				BlockIsRAM[p] = false;
			}
		}
	}

	private final void map_HiROMSRAM ()
	{
		map_index(0x20, 0x3f, 0x6000, 0x7fff, MAP_HIROM_SRAM, MAP_TYPE_RAM);
		map_index(0xa0, 0xbf, 0x6000, 0x7fff, MAP_HIROM_SRAM, MAP_TYPE_RAM);
	}

	private final void map_WRAM ()
	{
		// DJW: 0 was RAM pointer
		map_space(0x7e, 0x7e, 0x0000, 0xffff, 0);
		map_space(0x7f, 0x7f, 0x0000, 0xffff, 0 + 0x10000);
	}

	private final void map_WriteProtectROM ()
	{
		// DJW: memmove((void *) WriteMap, (void *) Map, sizeof(Map));
		System.arraycopy(Map, 0, WriteMap, 0, MEMMAP_NUM_BLOCKS);

		for (int c = 0; c < 0x1000; c++)
		{
			if (BlockIsROM[c])
				WriteMap[c] = MAP_NONE;
		}
	}

	private final void Map_ExtendedHiROMMap ()
	{
		map_System();

		map_hirom_offset(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize - 0x400000, 0x400000);
		map_hirom_offset(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize - 0x400000, 0x400000);
		map_hirom_offset(0x80, 0xbf, 0x8000, 0xffff, 0x400000, 0);
		map_hirom_offset(0xc0, 0xff, 0x0000, 0xffff, 0x400000, 0);

		map_HiROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SameGameHiROMMap ()
	{
		map_System();

		map_hirom_offset(0x00, 0x1f, 0x8000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_hirom_offset(0x20, 0x3f, 0x8000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);
		map_hirom_offset(0x40, 0x5f, 0x0000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_hirom_offset(0x60, 0x7f, 0x0000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);
		map_hirom_offset(0x80, 0x9f, 0x8000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_hirom_offset(0xa0, 0xbf, 0x8000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);
		map_hirom_offset(0xc0, 0xdf, 0x0000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_hirom_offset(0xe0, 0xff, 0x0000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);

		map_HiROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void map_DSP ()
	{

		switch (dsp1.maptype)
		{
			case DSP1.M_DSP1_LOROM_S:
				map_index(0x20, 0x3f, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xa0, 0xbf, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				break;

			case DSP1.M_DSP1_LOROM_L:
				map_index(0x60, 0x6f, 0x0000, 0x7fff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xe0, 0xef, 0x0000, 0x7fff, MAP_DSP, MAP_TYPE_I_O);
				break;

			case DSP1.M_DSP1_HIROM:
				map_index(0x00, 0x1f, 0x6000, 0x7fff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0x80, 0x9f, 0x6000, 0x7fff, MAP_DSP, MAP_TYPE_I_O);
				break;

			case DSP1.M_DSP2_LOROM:
				map_index(0x20, 0x3f, 0x6000, 0x6fff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0x20, 0x3f, 0x8000, 0xbfff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xa0, 0xbf, 0x6000, 0x6fff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xa0, 0xbf, 0x8000, 0xbfff, MAP_DSP, MAP_TYPE_I_O);
				break;

			case DSP1.M_DSP3_LOROM:
				map_index(0x20, 0x3f, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xa0, 0xbf, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				break;

			case DSP1.M_DSP4_LOROM:
				map_index(0x30, 0x3f, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				map_index(0xb0, 0xbf, 0x8000, 0xffff, MAP_DSP, MAP_TYPE_I_O);
				break;
		}
	}

	private final void Map_HiROMMap ()
	{
		map_System();

		map_hirom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_hirom(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize);
		map_hirom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);
		map_hirom(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize);

		if (globals.settings.DSP1Master)
			map_DSP();

		map_HiROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_JumboLoROMMap ()
	{
		// XXX: Which game uses this?
		map_System();

		map_lorom_offset(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize - 0x400000, 0x400000);
		map_lorom_offset(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize - 0x400000, 0x400000);
		map_lorom_offset(0x80, 0xbf, 0x8000, 0xffff, 0x400000, 0);
		map_lorom_offset(0xc0, 0xff, 0x0000, 0xffff, 0x400000, 0x200000);

		map_LoROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_NoMAD1LoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize);

		map_index(0x70, 0x7f, 0x0000, 0xffff, MAP_LOROM_SRAM, MAP_TYPE_RAM);
		map_index(0xf0, 0xff, 0x0000, 0xffff, MAP_LOROM_SRAM, MAP_TYPE_RAM);

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_ROM24MBSLoROMMap ()
	{
		// PCB: BSC-1A5M-01, BSC-1A7M-10
		map_System();

		map_lorom_offset(0x00, 0x1f, 0x8000, 0xffff, 0x100000, 0);
		map_lorom_offset(0x20, 0x3f, 0x8000, 0xffff, 0x100000, 0x100000);
		map_lorom_offset(0x80, 0x9f, 0x8000, 0xffff, 0x100000, 0x200000);
		map_lorom_offset(0xa0, 0xbf, 0x8000, 0xffff, 0x100000, 0x100000);

		map_LoROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SRAM512KLoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize);

		map_space(0x70, 0x70, 0x0000, 0xffff, SRAM.getOffset());
		map_space(0x71, 0x71, 0x0000, 0xffff, SRAM.getOffset() + 0x8000);
		map_space(0x72, 0x72, 0x0000, 0xffff, SRAM.getOffset() + 0x10000);
		map_space(0x73, 0x73, 0x0000, 0xffff, SRAM.getOffset() + 0x18000);

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SufamiTurboPseudoLoROMMap ()
	{
		// for combined images
		map_System();

		map_lorom_offset(0x00, 0x1f, 0x8000, 0xffff, 0x40000, 0);
		map_lorom_offset(0x20, 0x3f, 0x8000, 0xffff, 0x100000, 0x100000);
		map_lorom_offset(0x40, 0x5f, 0x8000, 0xffff, 0x100000, 0x200000);
		map_lorom_offset(0x80, 0x9f, 0x8000, 0xffff, 0x40000, 0);
		map_lorom_offset(0xa0, 0xbf, 0x8000, 0xffff, 0x100000, 0x100000);
		map_lorom_offset(0xc0, 0xdf, 0x8000, 0xffff, 0x100000, 0x200000);

		// I don't care :P
		map_space(0x60, 0x63, 0x8000, 0xffff, SRAM.getOffset() - 0x8000);
		map_space(0xe0, 0xe3, 0x8000, 0xffff, SRAM.getOffset() - 0x8000);
		map_space(0x70, 0x73, 0x8000, 0xffff, SRAM.getOffset() + 0x4000 - 0x8000);
		map_space(0xf0, 0xf3, 0x8000, 0xffff, SRAM.getOffset() + 0x4000 - 0x8000);

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void map_C4 ()
	{
		map_index(0x00, 0x3f, 0x6000, 0x7fff, MAP_C4, MAP_TYPE_I_O);
		map_index(0x80, 0xbf, 0x6000, 0x7fff, MAP_C4, MAP_TYPE_I_O);
	}

	private final void map_OBC1 ()
	{
		map_index(0x00, 0x3f, 0x6000, 0x7fff, MAP_OBC_RAM, MAP_TYPE_I_O);
		map_index(0x80, 0xbf, 0x6000, 0x7fff, MAP_OBC_RAM, MAP_TYPE_I_O);
	}

	private final void map_SetaRISC ()
	{
		map_index(0x00, 0x3f, 0x3000, 0x3fff, MAP_SETA_RISC, MAP_TYPE_I_O);
		map_index(0x80, 0xbf, 0x3000, 0x3fff, MAP_SETA_RISC, MAP_TYPE_I_O);
	}

	private final void Map_LoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize);

		if (settings.DSP1Master)
			map_DSP();
		/*
		else if (settings.C4)
			map_C4();
		else if (settings.OBC1)
			map_OBC1();
		else // TODO: DJW, add when we have ST support
		if (Settings.SETA == ST_018)
			map_SetaRISC();
		*/
		map_LoROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SufamiTurboLoROMMap ()
	{
		map_System();

		map_lorom_offset(0x00, 0x1f, 0x8000, 0xffff, 0x40000, 0);
		map_lorom_offset(0x20, 0x3f, 0x8000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_lorom_offset(0x40, 0x5f, 0x8000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);
		map_lorom_offset(0x80, 0x9f, 0x8000, 0xffff, 0x40000, 0);
		map_lorom_offset(0xa0, 0xbf, 0x8000, 0xffff, globals.Multi.cartSizeA, globals.Multi.cartOffsetA);
		map_lorom_offset(0xc0, 0xdf, 0x8000, 0xffff, globals.Multi.cartSizeB, globals.Multi.cartOffsetB);

		if (Multi.sramSizeA > 0)
		{
			map_index(0x60, 0x63, 0x8000, 0xffff, MAP_LOROM_SRAM, MAP_TYPE_RAM);
			map_index(0xe0, 0xe3, 0x8000, 0xffff, MAP_LOROM_SRAM, MAP_TYPE_RAM);
		}

		if (Multi.sramSizeB > 0)
		{
			map_index(0x70, 0x73, 0x8000, 0xffff, MAP_LOROM_SRAM_B, MAP_TYPE_RAM);
			map_index(0xf0, 0xf3, 0x8000, 0xffff, MAP_LOROM_SRAM_B, MAP_TYPE_RAM);
		}

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SDD1LoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);

		map_hirom_offset(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize, 0);
		map_hirom_offset(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize, 0); // will be overwritten dynamically

		map_index(0x70, 0x7f, 0x0000, 0x7fff, MAP_LOROM_SRAM, MAP_TYPE_RAM);

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SA1LoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);

		map_hirom_offset(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize, 0);

		//map_space(0x00, 0x3f, 0x3000, 0x3fff, FillRAM);
		map_space(0x00, 0x3f, 0x3000, 0x3fff, FillRamOffset );
		map_space(0x80, 0xbf, 0x3000, 0x3fff, FillRamOffset );
		map_index(0x00, 0x3f, 0x6000, 0x7fff, MAP_BWRAM, MAP_TYPE_I_O);
		map_index(0x80, 0xbf, 0x6000, 0x7fff, MAP_BWRAM, MAP_TYPE_I_O);

		for (int c = 0x40; c < 0x80; c++)
			map_space(c, c, 0x0000, 0xffff, SRAM.getOffset() + (c & 1) * 0x10000);

		map_WRAM();

		map_WriteProtectROM();

		// Now copy the map and correct it for the SA1 CPU.

		//memmove((void *) SA1.Map, (void *) Map, sizeof(Map));
		System.arraycopy(Map, 0, sa1.Map, 0, MEMMAP_NUM_BLOCKS);
		//memmove((void *) SA1.WriteMap, (void *) WriteMap, sizeof(WriteMap));
		System.arraycopy(WriteMap, 0, sa1.WriteMap, 0, MEMMAP_NUM_BLOCKS);

		// SA-1 Banks 00->3f and 80->bf
		for (int c = 0x000; c < 0x400; c += 0x10)
		{
			sa1.Map[c + 0] = sa1.Map[c + 0x800] = FillRamOffset + 0x3000;
			sa1.Map[c + 1] = sa1.Map[c + 0x801] = MAP_NONE;
			sa1.WriteMap[c + 0] = sa1.WriteMap[c + 0x800] = FillRamOffset + 0x3000;
			sa1.WriteMap[c + 1] = sa1.WriteMap[c + 0x801] = MAP_NONE;
		}

		// SA-1 Banks 60->6f
		for (int c = 0x600; c < 0x700; c++)
			sa1.Map[c] = sa1.WriteMap[c] = MAP_BWRAM_BITMAP;

		BWRAM = SRAM;
	}


	private final void Map_SuperFXLoROMMap ()
	{
		map_System();

		// Replicate the first 2Mb of the ROM at ROM + 2MB such that each 32K
		// block is repeated twice in each 64K block.
		for (int c = 0; c < 64; c++)
		{
			// DJW memmove(&ROM[0x200000 + c * 0x10000], &ROM[c * 0x8000], 0x8000);
			ROM.arraycopy(0x200000 + c * 0x10000 , ROM, c * 0x8000,  0x8000 );
			//memmove(&ROM[0x208000 + c * 0x10000], &ROM[c * 0x8000], 0x8000);
			ROM.arraycopy(0x208000 + c * 0x10000 , ROM, c * 0x8000,  0x8000 );
		}

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);

		map_hirom_offset(0x40, 0x7f, 0x0000, 0xffff, CalculatedSize, 0);
		map_hirom_offset(0xc0, 0xff, 0x0000, 0xffff, CalculatedSize, 0);

		map_space(0x00, 0x3f, 0x6000, 0x7fff, SRAM.getOffset() - 0x6000);
		map_space(0x80, 0xbf, 0x6000, 0x7fff, SRAM.getOffset() - 0x6000);
		map_space(0x70, 0x70, 0x0000, 0xffff, SRAM.getOffset());
		map_space(0x71, 0x71, 0x0000, 0xffff, SRAM.getOffset() + 0x10000);

		map_WRAM();

		map_WriteProtectROM();
	}

	private final void Map_SetaDSPLoROMMap ()
	{
		map_System();

		map_lorom(0x00, 0x3f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x40, 0x7f, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0x80, 0xbf, 0x8000, 0xffff, CalculatedSize);
		map_lorom(0xc0, 0xff, 0x8000, 0xffff, CalculatedSize);

		map_SetaDSP();

		map_LoROMSRAM();
		map_WRAM();

		map_WriteProtectROM();
	}

	private final void map_LoROMSRAM ()
	{
		map_index(0x70, 0x7f, 0x0000, 0x7fff, MAP_LOROM_SRAM, MAP_TYPE_RAM);
		map_index(0xf0, 0xff, 0x0000, 0x7fff, MAP_LOROM_SRAM, MAP_TYPE_RAM);
	}

	private final void map_SetaDSP ()
	{
		// where does the SETA chip access, anyway?
		// please confirm this?
		map_index(0x68, 0x6f, 0x0000, 0x7fff, MAP_SETA_DSP, MAP_TYPE_RAM);
		// and this!
		map_index(0x60, 0x67, 0x0000, 0x3fff, MAP_SETA_DSP, MAP_TYPE_I_O);

		// ST-0010:
		// map_index(0x68, 0x6f, 0x0000, 0x0fff, MAP_SETA_DSP, ?);
	}

	private final int checksum_calc_sum (ByteArrayOffset data, int length)
	{
		// uint16 sum = 0;
		int	sum = 0;

		for (int i = 0; i < length; i++) {
			sum = (sum + data.get8Bit(i)) & 0xFFFF;
			// simulates the bit roll over
		}

		return (sum);
	}
	private final int checksum_mirror_sum (ByteArrayOffset start, int[] length )
	{
		return checksum_mirror_sum( start, length, 0x800000 );
	}
	
	private final int checksum_mirror_sum (ByteArrayOffset start, int[] length, int mask )
	{
		// from NSRT
		while (! ( ( length[0] & mask) == mask) )
			mask >>>= 1;

		//System.out.println( " Mask: 0x" + Integer.toHexString( mask ) );
		int	part1 = checksum_calc_sum(start, mask);
		int	part2 = 0;

		int[] next_length = { (length[0] - mask) };
		if (next_length[0] > 0)
		{
			part2 = checksum_mirror_sum(start.getOffsetBuffer(mask), next_length, mask >>> 1);

			while (next_length[0] < mask)
			{
				next_length[0] += next_length[0];
				part2 += part2;
			}

			length[0] = mask + mask;
		}

		return (part1 + part2);
	}

	private final int Checksum_Calculate ()
	{
		// from NSRT
		int	sum = 0;

		/*
		if (settings.SPC7110)
		{
			sum = checksum_calc_sum(ROM, CalculatedSize);
			if (CalculatedSize == 0x300000)
				sum += sum;
		}
		else
		{
		*/
			if ( ( CalculatedSize & 0x7fff ) != 0 )
				sum = checksum_calc_sum(ROM, CalculatedSize);
			else
			{
				int[] length = { CalculatedSize };
				sum = checksum_mirror_sum(ROM, length);
			}
		//}

		return sum;
	}

	private final int caCRC32 (ByteArrayOffset array, int size)
	{
		return caCRC32 (array, size, 0xffffffff);
	}
	
	private final int caCRC32 (ByteArrayOffset array, int size, int crc32)
	{		
		for (int i = 0; i < size; i++)
			crc32 = ( (crc32 >>> 8) & 0x00FFFFFF ) ^ crc32Table[ ( crc32 ^ array.get8Bit(i) ) & 0xFF];

		return (~ crc32);
	}

	private final void SetupMemoryMaps() {

		//// Map memory and calculate checksum
		Map_Initialize();
		CalculatedChecksum = 0;

		if (HiROM)
		{
			/*if (Settings.SPC7110)
				Map_SPC7110HiROMMap();
			else*/
			if (ExtendedFormat != NOPE)
				Map_ExtendedHiROMMap();
			else
			if (Multi.cartType == 3)
				Map_SameGameHiROMMap();
			else
				Map_HiROMMap();
		}
		else
		{
			/*if (Settings.SETA && Settings.SETA != ST_018)
				Map_SetaDSPLoROMMap();
			else*/
			if (settings.SuperFX)
				Map_SuperFXLoROMMap();
			else
			if (settings.SA1)
				Map_SA1LoROMMap();
			/*
			else
			if (settings.SDD1)
				Map_SDD1LoROMMap();
			*/
			else
			if (ExtendedFormat != NOPE)
				Map_JumboLoROMMap();
			else
			if ( Arrays.equals( ROMName.getBytes(0,17), "WANDERERS FROM YS".getBytes() ) )
				Map_NoMAD1LoROMMap();
			else
			if ( Arrays.equals( ROMName.getBytes(0,17), "SOUND NOVEL-TCOOL".getBytes() )||
					Arrays.equals( ROMName.getBytes(0,17), "DERBY STALLION 96".getBytes() ) )
				Map_ROM24MBSLoROMMap();
			else
			if ( Arrays.equals( ROMName.getBytes(0,21), "THOROUGHBRED BREEDER3".getBytes() ) ||
					Arrays.equals( ROMName.getBytes(0,11), "RPG-TCOOL 2".getBytes() ) )
				Map_SRAM512KLoROMMap();
			else
			if ( Arrays.equals( ROMName.getBytes(0,19), "ADD-ON BASE CASSETE".getBytes() ) )
			{
				if (Multi.cartType == 4)
				{
					SRAMSize = Multi.sramSizeA;
					Map_SufamiTurboLoROMMap();
				}
				else
				{
					SRAMSize = 5;
					Map_SufamiTurboPseudoLoROMMap();
				}
			}
			else
				Map_LoROMMap();
		}
	}

	private static final int strlen( byte[] value ) {
		int count = 0;
		while( count < value.length  ) { 
			if ( value[count] == 0 ) {
				return count;
			}
			count++;
		}
		return count;
	}

	private final ByteArray Truncate( ByteArray value, int length ) {
		byte[] ROMBytes = value.getBytes();
		ROMBytes[length - 1] = 0;
		if ( strlen( ROMBytes ) > 0)
		{
			int p = strlen( ROMBytes );
			if (ROMBytes[p] > 21 && ROMBytes[20] == ' ')
				p = 21;
			while (p > 0 && ROMBytes[p - 1] == ' ')
				p--;
			ROMBytes[p] = 0;
			
		}
		// Get the length of the truncated value
		int len = strlen( ROMBytes );
		// Create a new temp place for it
		byte[] temp = new byte[len];
		// Copy the values minus the truncated values
		System.arraycopy(ROMBytes, 0, temp, 0, len);
		//return a new ByteBuffer
		return new ByteArray( temp );
	}

	private final static void memset( int[] src, int value, int len ) {
		for ( int i = 0; i < len ; i++ ) {
			src[i] = value;
		}
	}
 
	private final void ResetSpeedMap ()
	{
		memset(MemorySpeed, SnesSystem.SLOW_ONE_CYCLE, 0x1000);

		// Fast  - [00-3f|80-bf]:[2000-3fff|4200-5fff]
		// XSlow - [00-3f|80-bf]:[4000-41ff] see also Get/SetCPU()
		for (int i = 0; i < 0x400; i += 0x10)
		{
			MemorySpeed[i + 2] = MemorySpeed[0x800 + i + 2] = SnesSystem.ONE_CYCLE;
			MemorySpeed[i + 3] = MemorySpeed[0x800 + i + 3] = SnesSystem.ONE_CYCLE;
			MemorySpeed[i + 4] = MemorySpeed[0x800 + i + 4] = SnesSystem.ONE_CYCLE;
			MemorySpeed[i + 5] = MemorySpeed[0x800 + i + 5] = SnesSystem.ONE_CYCLE;
		}

		FixROMSpeed();
	}

	final void FixROMSpeed()
	{
		if (cpu.FastROMSpeed == 0)
		{
			cpu.FastROMSpeed = SnesSystem.SLOW_ONE_CYCLE;
		}

		// [80-bf]:[8000-ffff], [c0-ff]:[0000-ffff]
		for (int c = 0x800; c < 0x1000; c++)
		{
			if ( ( c & 0x8) == 0x8 || ( c & 0x400) == 0x400 ) {
				// DJW:  MemorySpeed[c] = (uint8) CPU.FastROMSpeed;
				MemorySpeed[c] = cpu.FastROMSpeed & 0xFF;
			}
		}
	}

	private final boolean match_na ( String str )
	{
		return Arrays.equals( ROMName.buffer, str.getBytes());
	}

	private final boolean match_nn ( String str)
	{
		return Arrays.equals( ROMName.getBytes( 0, str.length() ), str.getBytes());
	}

	private final boolean match_nc ( String str)
	{   
		String temp = ROMName.getString();
		temp.toUpperCase();
		return temp == str.toUpperCase();
	}

	private final boolean match_id ( String str)
	{
		return Arrays.equals( ROMId.getBytes(0, str.length() ), str.getBytes());
	}

	private final void ApplyROMFixes () throws Exception
	{
		//// APU timing hacks :(

		// This game cannot work well anyway
		if (match_id("AVCJ"))									  // Rendering Ranger R2
		{
			// TODO: Need to review this
			//IAPU.OneCycle = (int) (15.7 * (1 << SSystem.SNES_APU_ACCURACY));
			//printf("APU OneCycle hack: %d\n", IAPU.OneCycle);
		}

		// XXX: All Quintet games?
		if (match_na("GAIA GENSOUKI 1 JPN")					 || // Gaia Gensouki
			match_id("JG  ")									|| // Illusion of Gaia
			match_id("CQ  "))									  // Stunt Race FX
		{
//			 TODO: Need to review this
			//IAPU.OneCycle = (int) (13.0 * (1 << SSystem.SNES_APU_ACCURACY));
			//printf("APU OneCycle hack: %d\n", IAPU.OneCycle);
		}
		
		int[] sankoFever = { 0x53, 0x41, 0x4E, 0x4B, 0x59, 0x4F, 0x32, 0x46, 0x65, 0x76, 0x65, 0x72, 0x21, 0xCC, 0xA8, 0xB0, 0xCA, 0xDE, 0xB0, 0x21 };
		
		if (match_na("SOULBLADER - 1")						  || // Soul Blader
			match_na("SOULBLAZER - 1 USA")					  || // Soul Blazer
			match_na("SLAP STICK 1 JPN")						|| // Slap Stick
			match_id("E9 ")									 || // Robotrek
			match_nn("ACTRAISER")							   || // Actraiser
			match_nn("ActRaiser-2")							 || // Actraiser 2
			match_id("AQT")									 || // Tenchi Souzou, Terranigma
			match_id("ATV")									 || // Tales of Phantasia
			match_id("ARF")									 || // Star Ocean
			match_id("APR")									 || // Zen-Nippon Pro Wrestling 2 - 3-4 Budoukan
			match_id("A4B")									 || // Super Bomberman 4
			match_id("Y7 ")									 || // U.F.O. Kamen Yakisoban - Present Ban
			match_id("Y9 ")									 || // U.F.O. Kamen Yakisoban - Shihan Ban
			match_id("APB")									 || // Super Bomberman - Panic Bomber W
			match_na("DARK KINGDOM")							|| // Dark Kingdom
			match_na("ZAN3 SFC")								|| // Zan III Spirits
			match_na("HIOUDEN")								 || // Hiouden - Mamono-tachi Tono Chikai
			//match_na("\xC3\xDD\xBC\xC9\xB3\xC0")				|| // Tenshi no Uta
			match_na("FORTUNE QUEST")						   || // Fortune Quest - Dice wo Korogase
			match_na("FISHING TO BASSING")					  || // Shimono Masaki no Fishing To Bassing
			match_na("OHMONO BLACKBASS")						|| // Oomono Black Bass Fishing - Jinzouko Hen
			match_na("MASTERS")								 || // Harukanaru Augusta 2 - Masters
			//match_na("SFC \xB6\xD2\xDD\xD7\xB2\xC0\xDE\xB0")	|| // Kamen Rider
			match_na("ZENKI TENCHIMEIDOU")						|| // Kishin Douji Zenki - Tenchi Meidou
			match_nn("TokyoDome '95Battle 7")				   || // Shin Nippon Pro Wrestling Kounin '95 - Tokyo Dome Battle 7
			match_nn("SWORD WORLD SFC")						 || // Sword World SFC/2
			match_nn("LETs PACHINKO(")						  || // BS Lets Pachinko Nante Gindama 1/2/3/4
			match_nn("THE FISHING MASTER")					  || // Mark Davis The Fishing Master
			match_nn("Parlor")								  || // Parlor mini/2/3/4/5/6/7, Parlor Parlor!/2/3/4/5
			match_na("HEIWA Parlor!Mini8")					 ) //|| Parlor mini 8
			//match_Bytes( sankoFever )   // SANKYO Fever! Fever!
		{
			//TODO: Review
			//IAPU.OneCycle = (int) (15.0 * (1 << SSystem.SNES_APU_ACCURACY));
			//printf("APU OneCycle hack: %d\n", IAPU.OneCycle);
		}

		//// DMA/HDMA timing hacks :(

		timings.HDMAStart   = SnesSystem.SNES_HDMA_START_HC + settings.HDMATimingHack - 100;
		timings.HBlankStart = SnesSystem.SNES_HBLANK_START_HC + timings.HDMAStart - SnesSystem.SNES_HDMA_START_HC;

		// The delay to sync CPU and DMA which Sne cannot emulate.
		// Some games need really severe delay timing...
		if (match_na("BATTLE GRANDPRIX")) // Battle Grandprix
		{
			timings.DMACPUSync = 20;
			//printf("DMA sync: %d\n", Timings.DMACPUSync);
		}

		//// CPU speed-ups (CPU_Shutdown())

		// Force disabling a speed-up hack
		// Games which spool sound samples between the SNES and sound CPU using
		// H-DMA as the sample is playing.
		if (match_na("EARTHWORM JIM 2") || // Earth Worm Jim 2
			match_na("PRIMAL RAGE")	 || // Primal Rage
			match_na("CLAY FIGHTER")	|| // Clay Fighter
			match_na("ClayFighter 2")   || // Clay Fighter 2
			match_na("WeaponLord")	  || // Weapon Lord
			match_nn("WAR 2410")		|| // War 2410
			match_id("ARF")			 || // Star Ocean
			match_id("A4WJ")			|| // Mini Yonku Shining Scorpion - Let's & Go!!
			match_nn("NHL")			 ||
			match_nc("MADDEN"))
		{
			//if (Settings.Shutdown)
				//printf("Disabled CPU shutdown hack.\n");
			settings.Shutdown = false;
		}

		// SA-1
		sa1.WaitAddress = 0xffffffff;
		// DJW, No assignment up to this point
		//SA1.WaitByteAddress1 = NULL;
		//SA1.WaitByteAddress2 = NULL;

		if (settings.SA1)
		{
			// Kirby Super Star (U)
			if (match_id("AKFE"))
			{
				sa1.WaitAddress = 0x008cb8;
				sa1.WaitByteAddress1 = FillRamOffset + 0x300a;
				sa1.WaitByteAddress2 = FillRamOffset + 0x300e;
			}

			// Super Mario RPG (J), (U)
			if (match_id("ARWJ") || match_id("ARWE"))
			{
				sa1.WaitAddress = 0xc0816f;
				sa1.WaitByteAddress1 = FillRamOffset + 0x3000;
			}

			// Marvelous (J)
			if (match_id("AVRJ"))
			{
				sa1.WaitAddress = 0x0085f2;
				sa1.WaitByteAddress1 = FillRamOffset + 0x3024;
			}

			// PGA European Tour (U)
			if (match_id("AEPE"))
			{
				sa1.WaitAddress = 0x003700;
				sa1.WaitByteAddress1 = FillRamOffset + 0x3102;
			}

			// PGA Tour 96 (U)
			if (match_id("A3GE"))
			{
				sa1.WaitAddress = 0x003700;
				sa1.WaitByteAddress1 = FillRamOffset + 0x3102;
			}

			// Power Rangers Zeo - Battle Racers (U)
			if (match_id("A4RE"))
			{
				sa1.WaitAddress = 0x009899;
				sa1.WaitByteAddress1 = FillRamOffset + 0x3000;
			}

			// SD F-1 Grand Prix (J)
			if (match_id("AGFJ"))
			{
				sa1.WaitAddress = 0x0181bc;
			}
		}

		//// SRAM fixes

		if (match_na("HITOMI3"))
		{
			SRAMSize = 1;
			SRAMMask = ((1 << (SRAMSize + 3)) * 128) - 1;
		}

	}

	void InitROM () throws Exception
	{
		// TODO: Is this a superfx only?
		superfx.nRomBanks = CalculatedSize >>> 15;
	   
		// For the SPC7110 Chip 
		// TODO: Move this somewhere else?
		//s7r.DataRomSize = 0;

		// Get the offset of the ROM Header
		//System.out.println( "ROM Offset: " + ROM.getOffset() );
		ByteArrayOffset RomHeader = ROM.getOffsetBuffer(0x7FB0);
		
		//System.out.println( "RomHeader offset: " + RomHeader.getOffset() );
		//System.out.println( "RomHeader: " + RomHeader.getHex(0x00) );
		//System.out.println( "Value: " + ROM.getHex(0x7FB0) );
		
		if (ExtendedFormat == BIGFIRST)
			RomHeader.setOffset( 0x7FB0 + 0x400000 );
		if (HiROM)
			RomHeader.setOffset( 0x7FB0 + 0x8000 );

		// DJW: No Support for "Satellaview BS-X" Yet
		//InitBSX(); // Set BS header before parsing

		ParseSNESHeader( RomHeader );

		DecideWhichDSPToUse( RomHeader );

		// Detect and initialize chips included on the original cartridges
		// Detection codes are compatible with NSRT
		DetectOptionalChips( );

		SetupMemoryMaps( );

		CalculatedChecksum = Checksum_Calculate();

		isChecksumOK = (ROMChecksum + ROMComplementChecksum == 0xffff) &
							   (ROMChecksum == CalculatedChecksum);

		//// Build more ROM information
	
		ROMCRC32 = caCRC32(ROM, CalculatedSize);
		
		// NTSC/PAL
		if (settings.ForceNTSC)
			settings.PAL = false;
		else
		if (settings.ForcePAL)
			settings.PAL = true;
		/*
		else if (!settings.BS && (ROMRegion >= 2) && (ROMRegion <= 12))
			settings.PAL = true;
		*/
		else
			settings.PAL = false;

		if (settings.PAL)
		{
			settings.FrameTime = settings.FrameTimePAL;
			ROMFramesPerSecond = 50;
		}
		else
		{
			settings.FrameTime = settings.FrameTimeNTSC;
			ROMFramesPerSecond = 60;
		}

		ROMName = Truncate( ROMName, ROM_NAME_LEN );

		// SRAM size
		SRAMMask = ( SRAMSize > 0 )? ((1 << (SRAMSize + 3)) * 128) - 1 : 0;

		/*DJW
		// checksum
		if (!isChecksumOK || ((uint32) CalculatedSize > (uint32) (((1 << (ROMSize - 7)) * 128) * 1024)))
		{
			if (Settings.DisplayColor == 0xffff || Settings.DisplayColor != BUILD_PIXEL(31, 0, 0))
			{
				Settings.DisplayColor = BUILD_PIXEL(31, 31, 0);
				SET_UI_COLOR(255, 255, 0);
			}
		}*/

		/*if (Multi.cartType == 4)
		{
			Settings.DisplayColor = BUILD_PIXEL(0, 16, 31);
			SET_UI_COLOR(0, 128, 255);
		}*/

		//// Initialize emulation

		timings.H_Max_Master = SnesSystem.SNES_CYCLES_PER_SCANLINE;
		timings.H_Max		= timings.H_Max_Master;
		timings.HBlankStart  = SnesSystem.SNES_HBLANK_START_HC;
		timings.HBlankEnd	= SnesSystem.SNES_HBLANK_END_HC;
		timings.HDMAInit	 = SnesSystem.SNES_HDMA_INIT_HC;
		timings.HDMAStart	= SnesSystem.SNES_HDMA_START_HC;
		timings.RenderPos	= SnesSystem.SNES_RENDER_START_HC;
		timings.V_Max_Master = settings.PAL ? SnesSystem.SNES_MAX_PAL_VCOUNTER : SnesSystem.SNES_MAX_NTSC_VCOUNTER;
		timings.V_Max		= timings.V_Max_Master;
		/* From byuu: The total delay time for both the initial (H)DMA sync (to the DMA clock),
		   and the end (H)DMA sync (back to the last CPU cycle's mcycle rate (6, 8, or 12)) always takes between 12-24 mcycles.
		   Possible delays: { 12, 14, 16, 18, 20, 22, 24 }
		   XXX: Sne can't emulate this timing :( so let's use the average value... */
		timings.DMACPUSync   = 18;

		// NAC: Maybe put this in the APUReset?
		globals.apu.OneCycle = SnesSystem.SNES_APU_ONE_CYCLE_SCALED;

		cpu.FastROMSpeed = 0;
		ResetSpeedMap();

		ppu.TotalEmulatedFrames = 0;

		settings.Shutdown = settings.ShutdownMaster;

		//// Hack games

		ApplyROMFixes();

		//// Show ROM information
		if ( SnesSystem.DEBUG_MEM )
		{
			System.out.println( "ROM Name: " + ROMName.getString() );
			//System.out.println( "ROM Name: " + ROMName.getHexString(0, 23) );
			System.out.println( "ROM ID: " + ROMId.getHexString(0 , 4) );
			System.out.println( "ROM CRC32: " + Integer.toHexString(ROMCRC32 ).toUpperCase() );
			System.out.println( "CompanyId: " + CompanyId );
			System.out.println( "CheckSum: " + ( isChecksumOK ? "OK" : (Multi.cartType == 4) ? "NO Checksum" : "NOT OK") );
			System.out.println( "MapType: " + GetMapType() );
			System.out.println( "ROM Size: " + GetROMSizeMB() );
			System.out.println( "Chip Cartridge Required for ROM: " + CartridgeChip() );
			System.out.println( "MapMode: " + Integer.toHexString( ROMSpeed & ~0x10 ) );
			System.out.println( "Video Output: " + GetVideoMode() );
			System.out.println( "Static RAM Size: " + StaticRAMSize() );
		}
		//SnesSystem.PostRomInit();

		globals.controls.VerifyControllers();
	}

	private final String GetVideoMode() {
		return (settings.PAL ? "PAL" : "NTSC");
	}
	
	private final String GetMapType() {
		return (HiROM ? ((ExtendedFormat != NOPE) ? "ExHiROM": "HiROM") : "LoROM");
	}
	
	private final String StaticRAMSize ()
	{

		if (SRAMSize > 16)
			return "Corrupt";
		else
			return Integer.toString( (int) (SRAMMask + 1) / 1024 ) + "KB";

	}

	private final String CartridgeChip ()
	{
		String contents[] = { "ROM", "ROM+RAM", "ROM+RAM+BAT" };

		String	chip;
		
		if (settings.SA1)
			chip = "+SA-1";
		else
		if (settings.SuperFX)
			chip = "+SuperFX";
		else
		/*
		if (ROMType == 0 && !settings.BS)
			return "ROM";
		else
		if (settings.BS)
			chip = "+BSX";
		else
		if (settings.SDD1)
			chip = "+S-DD1";
		else
		if (settings.OBC1)
			chip = "+OBC1";
		else
		if (settings.SPC7110RTC)
			chip = "+SPC7110+RTC";
		else
		if (settings.SPC7110)
			chip = "+SPC7110";
		else
		if (settings.SRTC)
			chip = "+S-RTC";
		else
		if (settings.C4)
			chip = "+C4";
		else
		if (Settings.SETA == ST_010)
			chip = "+ST-010";
		else
		if (Settings.SETA == ST_011)
			chip = "+ST-011";
		else
		if (Settings.SETA == ST_018)
			chip = "+ST-018";
		else*/
		if (settings.DSP1Master)
			chip = "+DSP" + Integer.toString(dsp1.version + 1);
		else
			chip = "";

		return contents[(ROMType & 0xf) % 3] +  chip;

	}
	
	
	private final String GetROMSizeMB ()
	{
		if (Multi.cartType == 4)
		{
			return "N/A";
		}
		else
		{
			if (ROMSize < 7 || ROMSize - 7 > 23)
			{
				return "Corrupt";
			}
			else
			{
				return Integer.toString( 1 << (ROMSize - 7)) + "Mbits";
			}
		}

	}
	
	private void ClearSRAM( boolean onlyNonSavedSRAM)
	{
		if( onlyNonSavedSRAM )
		{
			// can have SRAM
			if( ! (globals.settings.SuperFX && ROMType < 0x15) && ! (globals.settings.SA1 && ROMType == 0x34)) 
			{
				return;
			}
		}

		globals.memory.SRAM.fill( SRAMInitialValue, 0, 0x20000);
	}

	boolean LoadSRAM( String filename)
	{
		SMulti Multi = globals.Multi;
		RTC rtc = globals.rtc;
		
		int	size;
		String sramName = new String(filename);

		// TODO: ClearSRAM
		//ClearSRAM();

		if (Multi.cartType > 0 && Multi.sramSizeB > 0)
		{
			String temp = new String(globals.memory.ROMFilename);
			globals.memory.ROMFilename = new String(Multi.fileNameB);
			
			size = (1 << (Multi.sramSizeB + 3)) * 128;
			
			//TODO: GetFilename
			//File SRAMFile = new File(GetFilename(".srm", SRAM_DIR));
			File SRAMFile = new File("replace this with code above");

			InputStream is;
			try {
				is = new FileInputStream(SRAMFile);
				
				byte[] SRAMbytes = new byte[(int)SRAMFile.length()];
				
				int offset = 0, numRead = 0;
				while (offset < SRAMbytes.length
					   && (numRead=is.read(SRAMbytes, offset, SRAMbytes.length-offset)) >= 0) {
					offset += numRead;
				}
				is.close();
	
				
				Multi.sramB = new ByteArray(SRAMbytes);
		
				if (offset - size == 512)
					Multi.sramB.arraycopy(0, Multi.sramB, 512, size);
				
			} catch (FileNotFoundException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			globals.memory.ROMFilename = new String(temp);
		}

		size = globals.memory.SRAMSize > 0 ? (1 << (globals.memory.SRAMSize + 3)) * 128 : 0;
		if (size > 0x20000)
			size = 0x20000;

		if (size > 0)
		{
			File SRAMFile = new File(sramName);
	
			try
			{
				InputStream is = new FileInputStream(SRAMFile);
			
				byte[] SRAMbytes = new byte[(int)SRAMFile.length()];
				
				int offset = 0, numRead = 0;
				while (offset < SRAMbytes.length
					   && (numRead=is.read(SRAMbytes, offset, SRAMbytes.length-offset)) >= 0) {
					offset += numRead;
				}
				is.close();
				
				if (offset - size == 512)
					SRAM.arraycopy(0, SRAM, 512, size);

				if (offset == size + RTC.SRTC_SRAM_PAD)
				{
					globals.rtc.SRTCPostLoadState();
					globals.rtc.ResetSRTC();
					rtc.index = -1;
					rtc.mode = RTC.MODE_READ;
				}
				else
				{
					globals.rtc.HardResetSRTC();
				}

				/*
				if (globals.settings.SPC7110RTC)
				{
					//TODO: LoadSPC7110RTC
					//LoadSPC7110RTC(&rtc_f9);
				}
				*/

				return true;
			}
			catch (IOException e)
			{
				/*
				if (settings.BS && !settings.BSXItself)
				{
					// The BS game's SRAM was not found
					// Try to read BS-X.srm instead
					String path = "";

					// TODO: GetDirectory
					// path = GetDirectory(SRAM_DIR);
					
					path.concat(File.separator);
					path.concat("BS-X.srm");
	
					File file1 = new File(path);
					
					try 
					{
						InputStream is = new FileInputStream(file1);

						byte[] SRAMbytes = new byte[(int)SRAMFile.length()];
						
						int offset = 0, numRead = 0;
						while (offset < SRAMbytes.length
							   && (numRead=is.read(SRAMbytes, offset, SRAMbytes.length-offset)) >= 0) {
							offset += numRead;
						}
						is.close();

						if (offset - size == 512)
							SRAM.arraycopy(0, SRAM, 512, size);
						
						SnesSystem.Message(SnesSystem._INFO, SnesSystem._ROM_INFO, "The SRAM file wasn't found: BS-X.srm was read instead.");
						globals.rtc.HardResetSRTC();
						return true;
						
					}
					catch (IOException e1)
					{
						SnesSystem.Message(SnesSystem._INFO, SnesSystem._ROM_INFO, "The SRAM file wasn't found, BS-X.srm wasn't found either.");
						globals.rtc.HardResetSRTC();
						return false;
					}
				}*/
			}
			

			globals.rtc.HardResetSRTC();
			return false;
		}

		/*
		if (settings.SDD1)
		{
			//TODO: SDD1LoadLoggedData
			//SDD1LoadLoggedData();
		}
		*/

		return true;
	}
	
	boolean SaveSRAM( String filename)
	{		
		if (settings.SuperFX && ROMType < 0x15) // doesn't have SRAM
			return true;

		if (settings.SA1 && ROMType == 0x34)	// doesn't have SRAM
			return true;

		int	size;
		String sramName = new String(filename);

		if (Multi.cartType > 0 && Multi.sramSizeB > 0)
		{
			String temp = new String(globals.memory.ROMFilename);
			globals.memory.ROMFilename = new String(Multi.fileNameB);
			
			//TODO: GetFilename
			//String name = SSystem.GetFilename(".srm", SRAM_DIR);
			String name = "remove me";
			
			size = (1 << (Multi.sramSizeB + 3)) * 128;

			try
			{
				OutputStream os = new FileOutputStream(name);
				os.write(Multi.sramB.buffer);
				os.close();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			globals.memory.ROMFilename = temp;
		}

		size = globals.memory.SRAMSize > 0 ? (1 << (globals.memory.SRAMSize + 3)) * 128 : 0;

		if (settings.SRTC)
		{
			size += RTC.SRTC_SRAM_PAD;
			globals.rtc.SRTCPreSaveState();
		}

		/*
		if (settings.SDD1)
		{
			// TODO: SDD1SaveLoggedData
			//SDD1SaveLoggedData();
		}
		*/

		if (size > 0x20000)
			size = 0x20000;

		if (size > 0)
		{
			try
			{
				OutputStream os = new FileOutputStream(sramName);
				os.write(SRAM.buffer);
				os.close();
				
				/*
				if (settings.SPC7110RTC)
				{
					// TODO: SaveSPC7110RTC
					//SaveSPC7110RTC(&rtc_f9);
				}
				*/

			} catch (FileNotFoundException e) {
			
			} catch (IOException e) {
				e.printStackTrace();
			}
				
			return true;
		}

		return false;
	}

	
	static boolean allASCII (ByteArrayOffset buf, int size)
	{
		for (int i = 0; i < size; i++)
		{
			if (buf.get8Bit(i) < 32 || buf.get8Bit(i) > 126)
				return false;
		}

		return true;
	}
}