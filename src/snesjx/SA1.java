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

final class SA1 extends WDC_65c816
{		
	boolean Executing;
		
	boolean Waiting;
	
	//private int PBPCAtOpcodeStart;
	
	int WaitAddress;

	int WaitCounter;

	int WaitByteAddress1;
	
	int WaitByteAddress2;
	
	int[] Map = new int[Memory.MEMMAP_NUM_BLOCKS];
	
	int[] WriteMap = new int[Memory.MEMMAP_NUM_BLOCKS];

	private int op1;
	
	private int op2;
	
	private int arithmetic_op;
	
	private long sum;
	
	//private boolean overflow;
	
	private int VirtualBitmapFormat;
	
	int in_char_dma;
	
	private int variable_bit_pos;
	
	private int SA1OpenBus;
	
	private static final int SNES_IRQ_SOURCE = (1 << 7);
	private static final int TIMER_IRQ_SOURCE = (1 << 6);
	private static final int DMA_IRQ_SOURCE = (1 << 5);
	
	private CPU cpu;
	private Memory memory;
	
	void setUp()
	{
		super.setUp();
		cpu = globals.cpu; 
		memory = globals.memory;
	}
	
	void SA1Init()
	{
		NMIActive = false;
		IRQActive = 0;
		WaitingForInterrupt = false;
		Waiting = false;
		Flags = 0;
		Executing = false;
		
		memory.FillRAM.fill(0, 0x2200, 0x200);
		
		memory.FillRAM.put8Bit(0x2200, 0x20);
		memory.FillRAM.put8Bit(0x2220, 0x00);
		memory.FillRAM.put8Bit(0x2221, 0x01);
		memory.FillRAM.put8Bit(0x2222, 0x02);
		memory.FillRAM.put8Bit(0x2223, 0x03);
		memory.FillRAM.put8Bit(0x2228, 0xff);
		
		op1 = 0;
		op2 = 0;
		arithmetic_op = 0;
		sum = 0;
		//overflow = false;
		
	}

	private void SA1Reset ()
	{
		PBPC(0);
		
		PCw = memory.FillRAM.get8Bit(0x2203) | (memory.FillRAM.get8Bit(0x2204) << 8);
		D = 0;
		DB = 0;
		S = (S & 0xFF00 ) | (0xFF);
		X = (X & 0x00FF);
		Y = (Y & 0x00FF);
		PL = 0;

		ShiftedPB = 0;
		ShiftedDB = 0;
		
		SetFlags (MemoryFlag | IndexFlag | IRQ, Emulation);
		PL = PL & (~Decimal & 0xFF);

		WaitingForInterrupt = false;
		SetPCBase( PBPC() );

		UnpackStatus();
		FixCycles();
		
		Executing = true;
		BWRAM = memory.SRAM;
		memory.FillRAM.put8Bit(0x2225, 0);
	}
	
	private void SetBWRAMMemMap(int val)
	{
		if ( ( val & 0x80 ) >  0 )
		{
			for (int c = 0; c < 0x400; c += 16)
			{
				Map [c + 6] = Map [c + 0x806] = Memory.MAP_BWRAM_BITMAP2;
				Map [c + 7] = Map [c + 0x807] = Memory.MAP_BWRAM_BITMAP2;
				WriteMap [c + 6] = WriteMap [c + 0x806] = Memory.MAP_BWRAM_BITMAP2;
				WriteMap [c + 7] = WriteMap [c + 0x807] = Memory.MAP_BWRAM_BITMAP2;
			}
			BWRAM = memory.SRAM.getOffsetBuffer( (val & 0x7f) * 0x2000 / 4 );
		}
		else
		{
			for (int c = 0; c < 0x400; c += 16)
			{
				Map [c + 6] = Map [c + 0x806] = Memory.MAP_BWRAM;
				Map [c + 7] = Map [c + 0x807] = Memory.MAP_BWRAM;
				WriteMap [c + 6] = WriteMap [c + 0x806] = Memory.MAP_BWRAM;
				WriteMap [c + 7] = WriteMap [c + 0x807] = Memory.MAP_BWRAM;
			}
			
			BWRAM = memory.SRAM.getOffsetBuffer( (val & 7) * 0x2000);
		}
	}

	void FixAfterSnapshotLoad()
	{
		ShiftedPB = PB() << 16;
		ShiftedDB = DB << 16;

		SetPCBase(PBPC());
		UnpackStatus();
		FixCycles();

		VirtualBitmapFormat = (memory.FillRAM.getByte(0x223f) & 0x80) > 0 ? 2 : 4;
		memory.BWRAM = memory.SRAM.getOffsetBuffer( (memory.FillRAM.getByte(0x2224) & 7) * 0x2000 );
		
		SetBWRAMMemMap( memory.FillRAM.get8Bit(0x2225) );

		Waiting = (memory.FillRAM.getByte(0x2200) & 0x60) != 0;
		Executing = !Waiting;
	}

	int GetByte( int address )
	{
		
		int GetAddress = Map[ ( address & 0xffffff ) >> Memory.MEMMAP_SHIFT ];
		
		if (GetAddress <= Memory.MAP_LAST)
		{
			return memory.RAM.get8Bit(GetAddress + (address & 0xffff));
		}

		switch ( GetAddress )
		{
		case Memory.MAP_PPU:
			return GetCPU (address & 0xffff);
		case Memory.MAP_LOROM_SRAM:
		case Memory.MAP_SA1RAM:
			return memory.SRAM.get8Bit(address & 0xffff);
		case Memory.MAP_BWRAM:
			return BWRAM.get8Bit((address & 0x7fff) - 0x6000);
		case Memory.MAP_BWRAM_BITMAP:
			address -= 0x600000;
			if (VirtualBitmapFormat == 2)
			{
				return (memory.SRAM.get8Bit((address >> 2) & 0xffff) >> ((address & 3) << 1)) & 3;
			}
			else
			{
				return (memory.SRAM.get8Bit((address >> 1) & 0xffff) >> ((address & 1) << 2)) & 15;
			}
		case Memory.MAP_BWRAM_BITMAP2:
			address = (address & 0xffff) - 0x6000;
			if (VirtualBitmapFormat == 2)
			{
				return (BWRAM.get8Bit((address >> 2) & 0xffff) >> ((address & 3) << 1)) & 3;
			}
			else
			{
				return (BWRAM.get8Bit((address >> 1) & 0xffff) >> ((address & 1) << 2)) & 15;
			}
		case Memory.MAP_DEBUG:
		default:
			return SA1OpenBus;
		}
	}
	
	int GetWord( int address )
	{
		return GetWord( address, Memory.WRAP_NONE );
	}
	
	int GetWord( int address, int w )
	{
		SA1OpenBus = GetByte( address );
		
		switch(w)
		{
		case Memory.WRAP_PAGE:
		{
			address = (address & 0xFFFF00 ) | ( ( address + 1 ) & 0xFF);
			return (SA1OpenBus | (GetByte( address) << 8 ) );
		}
		case Memory.WRAP_BANK:
		{
			address = (address & 0xFF0000 ) | ( ( address + 1 ) & 0xFFFF);
			return (SA1OpenBus | (GetByte( address) << 8) );
		}
		case Memory.WRAP_NONE:
		default:
			return (SA1OpenBus | (GetByte( address + 1) << 8));
		}
	}

	void SetByte(int Byte, int address)
	{
		int Setaddress = WriteMap [( address & 0xffffff ) >> Memory.MEMMAP_SHIFT];

		if (Setaddress <= Memory.MAP_LAST)
		{
			memory.RAM.put8Bit(address & 0xffff, Byte);
			return;
		}

		switch ( Setaddress )
		{
		case Memory.MAP_PPU:
			SetCPU( Byte, address & 0xffff);
			return;
		case Memory.MAP_SA1RAM:
		case Memory.MAP_LOROM_SRAM:
			memory.SRAM.put8Bit(address & 0xffff, Byte);
			return;
		case Memory.MAP_BWRAM:
			BWRAM.put8Bit((address & 0x7fff) - 0x6000, Byte);
			return;
		case Memory.MAP_BWRAM_BITMAP:
			address -= 0x600000;
			if (VirtualBitmapFormat == 2)
			{
				int ptr = memory.SRAM.get8Bit((address >> 2) & 0xffff);
				ptr &= ~(3 << ((address & 3) << 1));
				ptr |= (Byte & 3) << ((address & 3) << 1);
				
				memory.SRAM.put8Bit((address >> 2) & 0xffff, ptr);
			}
			else
			{
				int ptr = memory.SRAM.get8Bit((address >> 1) & 0xffff);
				ptr &= ~(15 << ((address & 1) << 2));
				ptr |= (Byte & 15) << ((address & 1) << 2);
				
				memory.SRAM.put8Bit((address >> 1) & 0xffff, ptr);
			}
			break;
		case Memory.MAP_BWRAM_BITMAP2:
			address = (address & 0xffff) - 0x6000;
			if (VirtualBitmapFormat == 2)
			{
				int ptr = BWRAM.get8Bit((address >> 2) & 0xffff);
				ptr &= ~(3 << ((address & 3) << 1));
				ptr |= (Byte & 3) << ((address & 3) << 1);
				BWRAM.put8Bit((address >> 2) & 0xffff, ptr);
			}
			else
			{
				int ptr = BWRAM.get8Bit((address >> 1) & 0xffff);
				ptr &= ~(15 << ((address & 1) << 2));
				ptr |= (Byte & 15) << ((address & 1) << 2);
				BWRAM.put8Bit((address >> 1) & 0xffff, ptr);
			}
		default:
			return;
		}
	}
	
	void SetWord(int Word, int address, int w, int o)
	{
		if( o == 0x0 ) SetByte (Word, address);
		
		switch(w)
		{
		case Memory.WRAP_PAGE:
		{
			int xPBPC = (address & 0xFFFF00 ) | ( ( address + 1 ) & 0xFF);
			SetByte( Word >> 8, xPBPC );
		}
		case Memory.WRAP_BANK:
		{
			int xPBPC = (address & 0xFF0000 ) | ( ( address + 1 ) & 0xFFFF);
			SetByte( Word >> 8, xPBPC );
		}
		case Memory.WRAP_NONE:
		default:
			SetByte( Word >> 8, address + 1 );
		}
		
		if(o > 0) SetByte ( Word, address);
	}

	void SetPCBase( int address )
	{
		PBPC(address & 0xffffff);
		ShiftedPB = address & 0xff0000;
		
		int GetAddress = Map [( address & 0xffffff ) >> Memory.MEMMAP_SHIFT];
		
		if (GetAddress <= Memory.MAP_LAST)
		{
			PCBase = memory.RAM.getOffsetBuffer(GetAddress);
			return;
		}
		
		switch ( GetAddress )
		{
		case Memory.MAP_SA1RAM:
			PCBase = memory.SRAM;
			return;
		
		case Memory.MAP_LOROM_SRAM:
			if( (memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
			{
				PCBase = null;
			} else {
				PCBase = memory.SRAM.getOffsetBuffer( 
						( ( ( address & 0xFF0000 ) >> 1 ) | ( address & 0x7FFF ) ) & 
						memory.SRAMMask - (address & 0xffff) );
			}
			return;
		
		case Memory.MAP_BWRAM:
			PCBase = BWRAM.getOffsetBuffer( 0x6000 - (address & 0x8000) );
			return;
		
		case Memory.MAP_HIROM_SRAM:
			if((memory.SRAMMask & Memory.MEMMAP_MASK ) != Memory.MEMMAP_MASK )
			{
				PCBase = null;
			} else {
				PCBase = memory.SRAM.getOffsetBuffer(
						( ( ( address & 0x7fff) - 0x6000 + ( ( address & 0xf0000 ) >> 3 ) ) & 
						memory.SRAMMask ) - (address&0xffff)
						);
			}
		return;
		
		case Memory.MAP_DEBUG:
		
		default:
		case Memory.MAP_NONE:
			PCBase = null;
			return;
		}
	}

	private void SetMemMap( int which1, int map)
	{
		int start = which1 * 0x100 + 0xc00;
		int start2 = which1 * 0x200;
	
		if (which1 >= 2)
			start2 += 0x400;
	
		for (int c = 0; c < 0x100; c += 16)
		{
			int block = memory.ROM.getOffset() + ((map & 7) * 0x100000 + (c << 12));
		
			for (int i = c; i < c + 16; i++)
			{
				memory.Map [start + i] = Map[start + i] = block;
			}
		}
		
		for (int c = 0; c < 0x200; c += 16)
		{
			int block = memory.ROM.getOffset() + (map & 7) * 0x100000 + (c << 11) - 0x8000;
			
			for (int i = c + 8; i < c + 16; i++)
				memory.Map [start2 + i] = Map [start2 + i] = block;
		}
	}
	
	int GetCPU( int address )
	{
		switch (address)
		{
		case 0x2300:
			return (((memory.FillRAM.getByte(0x2209) & 0x5f) |
			 (cpu.IRQActive & (PPU.SA1_IRQ_SOURCE | PPU.SA1_DMA_IRQ_SOURCE))));
		case 0x2301:
			return ((memory.FillRAM.getByte(0x2200) & 0xf) |
			(memory.FillRAM.getByte(0x2301) & 0xf0));
		case 0x2306:
			return (int)(sa1.sum) & 0xFF;
		case 0x2307:
			return (int)(sa1.sum >> 8) & 0xFF;
		case 0x2308:
			return (int)(sa1.sum >> 16) & 0xFF;
		case 0x2309:
			return (int)(sa1.sum >> 24) & 0xFF;
		case 0x230a:
			return (int)(sa1.sum >> 32) & 0xFF;
		case 0x230c:
			return memory.FillRAM.get8Bit(0x230c);
		case 0x230d:
		{
			int Byte = memory.FillRAM.get8Bit(0x230d);

			if ( ( memory.FillRAM.getByte(0x2258) & 0x80 ) != 0 )
			{
				ReadVariableLengthData(true, false);
			}
			return Byte;
		}
		default:

			break;
		}
		return memory.FillRAM.get8Bit(address);
	}
	
	void SetCPU(int Byte, int address)
	{
		switch( address )
		{
		case 0x2200:
			sa1.Waiting = (Byte & 0x60) != 0;

			if ( (Byte & 0x20) == 0 && (memory.FillRAM.getByte(0x2200) & 0x20) != 0)
			{
				SA1Reset ();
			}

			if ( ( Byte & 0x80 ) != 0)
			{
				memory.FillRAM.buffer[0x2301] |= 0x80;
				
				if ( ( memory.FillRAM.getByte(0x220a) & 0x80 ) != 0 )
				{
					Flags |= SnesSystem.IRQ_FLAG;
					IRQActive |= SNES_IRQ_SOURCE;
					Executing = !Waiting;
				}
			}
			
			if ( ( Byte & 0x10 ) > 0 )
			{
				memory.FillRAM.buffer[0x2301] |= 0x10;
			}
			break;

		case 0x2201:
			if (((Byte ^ memory.FillRAM.get8Bit(0x2201) ) & 0x80) != 0 &&
				(memory.FillRAM.getByte(0x2300) & Byte & 0x80) != 0)
			{
				cpu.SetIRQ(PPU.SA1_IRQ_SOURCE);
			}
			if (((Byte ^ memory.FillRAM.get8Bit(0x2201) ) & 0x20) > 0 &&
				(memory.FillRAM.getByte(0x2300) & Byte & 0x20) > 0)
			{
				cpu.SetIRQ(PPU.SA1_DMA_IRQ_SOURCE);
			}
			break;
		case 0x2202:
			if ( (Byte & 0x80 ) != 0 )
			{
				memory.FillRAM.buffer[0x2300] &= ~0x80;
				cpu.ClearIRQ(PPU.SA1_IRQ_SOURCE);
			}
			if ( ( Byte & 0x20 ) != 0 )
			{
				memory.FillRAM.buffer[0x2300] &= ~0x20;
				cpu.ClearIRQ(PPU.SA1_DMA_IRQ_SOURCE);
			}
			break;
		case 0x2203: break;
		case 0x2204: break;
		case 0x2205: break;
		case 0x2206: break;

		case 0x2207: break;
		case 0x2208: break;

		case 0x2209:
			memory.FillRAM.put8Bit(0x2209, Byte);
			if ( ( Byte & 0x80 ) != 0 )
				memory.FillRAM.buffer[0x2300] |= 0x80;
	
			if ( ( Byte & memory.FillRAM.getByte(0x2201) & 0x80 ) != 0 )
			{
				cpu.SetIRQ(PPU.SA1_IRQ_SOURCE);
			}
			break;
			
		case 0x220a:
			if (((Byte ^ memory.FillRAM.getByte(0x220a)) & 0x80) != 0 &&
				(memory.FillRAM.getByte(0x2301) & Byte & 0x80) != 0)
			{
				Flags |= SnesSystem.IRQ_FLAG;
				IRQActive |= SNES_IRQ_SOURCE;
			}
			if (((Byte ^ memory.FillRAM.get8Bit(0x220a)) & 0x40) != 0 &&
				(memory.FillRAM.getByte(0x2301) & Byte & 0x40) != 0)
			{
				Flags |= SnesSystem.IRQ_FLAG;
				IRQActive |= TIMER_IRQ_SOURCE;
			}
			if (((Byte ^ memory.FillRAM.get8Bit(0x220a)) & 0x20) != 0 &&
				(memory.FillRAM.getByte(0x2301) & Byte & 0x20) != 0)
			{
				Flags |= SnesSystem.IRQ_FLAG;
				IRQActive |= DMA_IRQ_SOURCE;
			}
			if (((Byte ^ memory.FillRAM.get8Bit(0x220a)) & 0x10) != 0 &&
				(memory.FillRAM.getByte(0x2301) & Byte & 0x10) != 0)
			{

			}
			break;
		case 0x220b:
			if ( ( Byte & 0x80 ) != 0 )
			{
				IRQActive &= ~SNES_IRQ_SOURCE;
				memory.FillRAM.buffer[0x2301] &= ~0x80;
			}
			if ( ( Byte & 0x40 ) != 0 )
			{
				IRQActive &= ~TIMER_IRQ_SOURCE;
				memory.FillRAM.buffer[0x2301] &= ~0x40;
			}
			if ( ( Byte & 0x20 ) != 0 )
			{
				IRQActive &= ~DMA_IRQ_SOURCE;
				memory.FillRAM.buffer[0x2301] &= ~0x20;
			}
			if ( ( Byte & 0x10 ) != 0 )
			{
				// Clear NMI
				memory.FillRAM.buffer[0x2301] &= ~0x10;
			}
			if (IRQActive == 0)
				Flags &= ~SnesSystem.IRQ_FLAG;
			break;
		case 0x220c: break;
		case 0x220d: break;

		case 0x220e: break;
		case 0x220f: break;

		case 0x2210: break;
		case 0x2211: break;
		case 0x2212: break;
		case 0x2213: break;
		case 0x2214: break;
		case 0x2215: break;
		case 0x2220:
		case 0x2221:
		case 0x2222:
		case 0x2223:
			SetMemMap (address - 0x2220, Byte);
			break;
		case 0x2224:
			memory.BWRAM = memory.SRAM.getOffsetBuffer( (Byte & 7) * 0x2000 );
			break;
		case 0x2225:
			if (Byte != memory.FillRAM.get8Bit(address))
				SetBWRAMMemMap (Byte);
			break;
		case 0x2226: break;
		case 0x2227: break;
		case 0x2228: break;
		case 0x2229: break;
		case 0x222a: break;
		case 0x2230: break;
		case 0x2231:
			if ( ( Byte & 0x80 ) == 0x80 )
				in_char_dma = 0;
			break;
		case 0x2232:
		case 0x2233:
		case 0x2234:
			memory.FillRAM.put8Bit(address, Byte);
			break;
		case 0x2235:
			memory.FillRAM.put8Bit(address, Byte);
			break;
		case 0x2236:
			memory.FillRAM.put8Bit(address, Byte);
			if ((memory.FillRAM.getByte(0x2230) & 0xa4) != 0 )
			{
				// Normal DMA to I-RAM
				DMA();
			}
			else if ((memory.FillRAM.getByte(0x2230) & 0xb0) != 0 )
			{
				memory.FillRAM.buffer[0x2300] |= 0x20;
				if ( ( memory.FillRAM.getByte(0x2201) & 0x20 ) != 0 )
				{
					cpu.SetIRQ(PPU.SA1_DMA_IRQ_SOURCE);
				}
				in_char_dma = 1;
			}
			break;
		case 0x2237:
			memory.FillRAM.put8Bit(address, Byte);
			if ((memory.FillRAM.getByte(0x2230) & 0xa4) == 0x84)
			{
				// Normal DMA to BW-RAM
				DMA ();
			}
			break;
		case 0x2238:
		case 0x2239:
			memory.FillRAM.put8Bit(address, Byte);
			break;
		case 0x223f:
			VirtualBitmapFormat = (Byte & 0x80) != 0 ? 2 : 4;
			break;
		case 0x2240:	case 0x2241:	case 0x2242:	case 0x2243:
		case 0x2244:	case 0x2245:	case 0x2246:	case 0x2247:
		case 0x2248:	case 0x2249:	case 0x224a:	case 0x224b:
		case 0x224c:	case 0x224d:	case 0x224e:	
			memory.FillRAM.put8Bit(address, Byte);
			break;

		case 0x224f:
			memory.FillRAM.put8Bit(address, Byte);
			if ((memory.FillRAM.getByte(0x2230) & 0xb0) == 0xa0)
			{
				// Char conversion 2 DMA enabled
				memory.ROM.arraycopy( (Memory.MAX_ROM_SIZE - 0x10000) + in_char_dma * 16, memory.FillRAM, 0x2240, 16);
				
				in_char_dma = (in_char_dma + 1) & 7;
				
				if ((in_char_dma & 3) == 0)
				{
					CharConv2();
				}
			}
			break;
		case 0x2250:
			if ( ( Byte & 2 ) == 2 )
				sum = 0;
			arithmetic_op = Byte & 3;
			break;

		case 0x2251:
			op1 = (op1 & 0xff00) | Byte;
			break;
		case 0x2252:
			op1 = (op1 & 0xff) | (Byte << 8);
			break;
		case 0x2253:
			op2 = (op2 & 0xff00) | Byte;
			break;
		case 0x2254:
			op2 = (op2 & 0xff) | (Byte << 8);
			switch (arithmetic_op)
			{
			case 0:	// multiply
			   	sum = op1 * op2;
			   	break;
			case 1: // divide
				if (op2 == 0)
				{
					sum = op1 << 16;
				}
				else
				{
					sum = (op1 / (int) (0xFFFF & op2)) | ((op1 % (int) (0xFFFF & op2)) << 16);
				}
				break;
			case 2:
			default: // cumulative sum
				sum += op1 * op2;
				if ( ( sum & ((long) 0xffffff << 32) ) > 0 )
				{
					//overflow = true;
				}
				break;
			}
			break;
		case 0x2258:	// Variable bit-field length/auto inc/start.
			memory.FillRAM.put8Bit(0x2258, Byte);
			ReadVariableLengthData(true, false);
			return;
		case 0x2259:
		case 0x225a:
		case 0x225b:	// Variable bit-field start address
			memory.FillRAM.put8Bit(address, Byte);
			// XXX: ???
			variable_bit_pos = 0;
			ReadVariableLengthData(false, true);
		return;

		default:
			break;
		}
		
		if (address >= 0x2200 && address <= 0x22ff)
			memory.FillRAM.put8Bit(address, Byte);
		
	}

	private void CharConv2()
	{
		int dest = memory.FillRAM.get8Bit(0x2235) | (memory.FillRAM.get8Bit(0x2236) << 8);
		int offset = (in_char_dma & 7) > 0 ? 0 : 1;
		int depth = (memory.FillRAM.getByte(0x2231) & 3) == 0 ? 8 : (memory.FillRAM.getByte(0x2231) & 3) == 1 ? 4 : 2;
		int bytes_per_char = 8 * depth;
		
		ByteArrayOffset p = memory.FillRAM.getOffsetBuffer(0x3000 + dest + offset * bytes_per_char);
		ByteArrayOffset q = memory.ROM.getOffsetBuffer( ( Memory.MAX_ROM_SIZE - 0x10000 ) + offset * 64);

		switch (depth)
		{
		case 2:
			break;
		case 4:
			break;
		case 8:
			for (int l = 0; l < 8; l++, q.setOffset(q.getOffset() + 8) )
			{
				for (int b = 0; b < 8; b++)
				{
					int r = q.get8Bit(b);
					
					p.put8Bit(0, p.get8Bit( (  0 << 1 ) | ( (r >> 0) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( (  1 << 1 ) | ( (r >> 1) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 16 << 1 ) | ( (r >> 2) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 17 << 1 ) | ( (r >> 3) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 32 << 1 ) | ( (r >> 4) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 33 << 1 ) | ( (r >> 5) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 48 << 1 ) | ( (r >> 6) & 1 ) ) );
					p.put8Bit(0, p.get8Bit( ( 49 << 1 ) | ( (r >> 7) & 1 ) ) );
				}
				p.setOffset(p.getOffset() + 2);
			}
			break;
		}
	}
	
	private void DMA()
	{
		int src =  memory.FillRAM.get8Bit(0x2232) | (memory.FillRAM.get8Bit(0x2233) << 8) | (memory.FillRAM.get8Bit(0x2234) << 16);
		int dst =  memory.FillRAM.get8Bit(0x2235) | (memory.FillRAM.get8Bit(0x2236) << 8) | (memory.FillRAM.get8Bit(0x2237) << 16);
		int len =  memory.FillRAM.get8Bit(0x2238) | (memory.FillRAM.get8Bit(0x2239) << 8);

		ByteArrayOffset s = null;
		ByteArrayOffset d = null;

		switch (memory.FillRAM.get8Bit(0x2230) & 3)
		{
		case 0: // ROM
			int address = Map[ (src & 0xffffff) >> Memory.MEMMAP_SHIFT ];
			
			s = memory.RAM.getOffsetBuffer( address );
			
			if (address <= Memory.MAP_LAST)
			{
				s.incOffset( src & 0xffff );
			}
			else
			{
				s = memory.ROM.getOffsetBuffer(src & 0xffff);
			}
			
			break;
		case 1: // BW-RAM
			src &= memory.SRAMMask;
			len &= memory.SRAMMask;
			s = memory.SRAM.getOffsetBuffer( src );
			break;
		default:
			case 2:
			src &= 0x3ff;
			len &= 0x3ff;
			s = memory.FillRAM.getOffsetBuffer(0x3000 + src);
			break;
		}

		if ( (memory.FillRAM.get8Bit(0x2230) & 4 ) == 4 )
		{
			dst &= memory.SRAMMask;
			len &= memory.SRAMMask;
			d = memory.SRAM.getOffsetBuffer( dst );
		}
		else
		{
			dst &= 0x3ff;
			len &= 0x3ff;
			d = memory.FillRAM.getOffsetBuffer(0x3000 + dst);
		}
		
		d.arraycopy(0, s, 0, d.size());
		
		memory.FillRAM.buffer[0x2301] |= 0x20;

		if ( ( memory.FillRAM.get8Bit(0x220a) & 0x20 ) != 0 )
		{
			sa1.Flags |= SnesSystem.IRQ_FLAG;
			sa1.IRQActive |= DMA_IRQ_SOURCE;
		}
	}
	
	private void ReadVariableLengthData( boolean inc, boolean no_shift )
	{
		int addr =  memory.FillRAM.get8Bit(0x2259) |
			  (memory.FillRAM.get8Bit(0x225a) << 8) |
			  (memory.FillRAM.get8Bit(0x225b) << 16);
		
		int shift = memory.FillRAM.get8Bit(0x2258) & 15;

		if (no_shift)
		{
			shift = 0;
		}
		else
		{
			if (shift == 0)
				shift = 16;
		}

		int s = shift + variable_bit_pos;

		if (s >= 16)
		{
			addr += (s >> 4) << 1;
			s &= 15;
		}
		
		int data = GetWord(addr) | (GetWord (addr + 2) << 16);

		data >>= s;
		memory.FillRAM.put8Bit(0x230c, data);
		memory.FillRAM.put8Bit(0x230d, (data >> 8));
		
		if (inc)
		{
			variable_bit_pos = (variable_bit_pos + shift) & 15;
			memory.FillRAM.put8Bit(0x2259, addr);
			memory.FillRAM.put8Bit(0x225a, addr >> 8);
			memory.FillRAM.put8Bit(0x225b, addr >> 16);
		}
	}

	void FixCycles()
	{
		if ( CheckEmulation() )
		{
			Opcodes = OpcodesM1X1;
			OpLengths = OpLengthsM1X1;
		}
		else if ( CheckMemory() )
		{
			if ( CheckIndex() )
			{
				Opcodes = OpcodesM1X1;
				OpLengths = OpLengthsM1X1;
			}
			else
			{
				Opcodes = OpcodesM1X0;
				OpLengths = OpLengthsM1X0;
			}
		}
		else
		{
			if ( CheckIndex() )
			{
				Opcodes = OpcodesM0X1;
				OpLengths = OpLengthsM0X1;
			}
			else
			{
				Opcodes = OpcodesM0X0;
				OpLengths = OpLengthsM0X0;
			}
		}
	}
	
	void SA1MainLoop ()
	{

	    if ( ( Flags & SnesSystem.IRQ_FLAG ) == SnesSystem.IRQ_FLAG )
	    {
	    	
		if ( IRQActive != 0)
		{
		    if (WaitingForInterrupt)
		    {
				WaitingForInterrupt = false;
				Incr8BitPC();
		    }
		    
		    if (!CheckFlag (IRQ))
		    {
		    	Opcode_IRQ();
		    }
		}
		else
		    Flags &= ~SnesSystem.IRQ_FLAG;
	    }
	    
	    // 
	    int OriginalOpcodes = Opcodes;
	    
	    for (int i = 0; i < 3 && sa1.Executing; i++)
	    {

	        PBPCAtOpcodeStart = PBPC();

	        int Op;
	        
	        if( PCBase != null)
	        {
	            SA1OpenBus = Op = PCBase.get8Bit(cpu.PCw);
	            Opcodes = OriginalOpcodes;
	        } else {
	            Op = GetByte( cpu.PBPC() );
	            Opcodes = OpcodesSlow;
	        }
	        
	        if( ( PCw & Memory.MEMMAP_MASK ) + OpLengths[Op] >= Memory.MEMMAP_BLOCK_SIZE)
	        {
	            int oldPC = PBPC();
	            
	            SetPCBase( PBPC() );
	            PBPC( oldPC );
	            Opcodes = OpcodesSlow;
	        }
	        
	        cpu.Incr8BitPC();
	        
	        ExecuteOp(Op);
	    }
	    
	    Opcodes = OriginalOpcodes;
	}
	
	protected void CPUShutdown()
	{
		if (globals.settings.Shutdown && PBPC() == WaitAddress)
		{
			if (sa1.WaitCounter >= 1)
			{
				sa1.Executing = false;
				sa1.CPUExecuting = 0;
			}
			else
				sa1.WaitCounter++;
		}
	}
}
