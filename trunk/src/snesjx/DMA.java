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

class DMA
{	
	class DMABank
	{
	     boolean ReverseTransfer;
	     boolean HDMAIndirectAddressing;
	     boolean UnusedBit43x0;
	     boolean AAddressFixed;
	     boolean AAddressDecrement;
	     int TransferMode;
	
	     int BAddress;
	     int AAddress;
	     int ABank;
	     int DMACount_Or_HDMAIndirectAddress;
	     int IndirectBank;
	     int Address;
	     boolean Repeat;
	     int LineCount;
	     int UnknownByte;
	     boolean DoTransfer;
	}
	
	DMABank[] dmabanks = new DMABank[8];
	
	ByteArrayOffset HDMAMemPointers[] = new ByteArrayOffset[8];
	//private ByteBufferOffset HDMABasePointers[] = new ByteBufferOffset[8];
	
	//modified per anomie Mode 5 findings
	private short[] HDMA_ModeByteCounts = {
	    1, 2, 2, 4, 4, 4, 2, 4
	};
	
	private Globals globals;
	private CPU cpu;
	private PPU ppu;
	private Memory memory;
	private Settings settings;
	private APU apu;
	private Timings timings;
	private SA1 sa1;
	
	void setUp()
	{
		globals = Globals.globals;
		cpu = globals.cpu;
		ppu = globals.ppu;
		apu = globals.apu;
		memory = globals.memory;
		settings = globals.settings;
		timings = globals.timings;
		sa1 = globals.sa1;
	}
    
	//private ByteArray sdd1_decode_buffer = new ByteArray(0x10000);
	
	// NAC: Unused
	/*
	private static int CompareSDD1IndexEntries (byte[] p1, byte[] p2)
	{
		return 0;
		// TODO: return (*(int *) p1 - *(int *) p2);
	}
	*/

	// Add 8 cycles per byte, sync APU, and do HC related events.
	// If HDMA was done in DoHEventProcessing(), check if it used the same channel as DMA.
	private boolean addCyclesInDMA (int dma_channel)
	{		
		cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
		apu.APUExecute();
		
		while (cpu.Cycles >= cpu.NextEvent)
		{
			globals.cpuexecutor.DoHEventProcessing();
		}

		if ( ( cpu.HDMARanInDMA & (1 << dma_channel) ) != 0 )
		{
			cpu.HDMARanInDMA = 0;

			// If HDMA triggers in the middle of DMA transfer and it uses the same channel,
			// it kills the DMA transfer immediately. $43x2 and $43x5 stop updating.
			return false;
		}

		cpu.HDMARanInDMA = 0;
		return true;
	}

	boolean DoDMA (int Channel)
	{
		//DD1 dd1 = globals.dd1;
		
		cpu.InDMA = true;
		cpu.InDMAorHDMA = true;

		DMABank dmabank = dmabanks[Channel];

		// Check invalid DMA first
		if ((dmabank.ABank == 0x7E || dmabank.ABank == 0x7F) && dmabank.BAddress == 0x80 && !dmabank.ReverseTransfer)
		{
			// Attempting a DMA from WRAM to $2180 will not work, WRAM will not be written.
			// Attempting a DMA from $2180 to WRAM will similarly not work,
			// the value written is (initially) the OpenBus value.
			// In either case, the address in $2181-3 is not incremented.

			// Does an invalid DMA actually take time?
			// I'd say yes, since 'invalid' is probably just the WRAM chip
			// not being able to read and write itself at the same time
			// And no, PPU.WRAM should not be updated.

			int	c = dmabank.DMACount_Or_HDMAIndirectAddress;
			// Writing $0000 to $43x5 actually results in a transfer of $10000 bytes, not 0.
			if (c == 0)
				c = 0x10000;

			// 8 cycles per channel
			cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
			// 8 cycles per byte
			while (c > 0)
			{
				dmabank.DMACount_Or_HDMAIndirectAddress--;
				dmabank.AAddress++;
				c--;
				if ( ! addCyclesInDMA(Channel) )
				{
					cpu.InDMA = false;
					cpu.InDMAorHDMA = false;
					return false;
				}
			}
			
			if (SnesSystem.DEBUG_DMA)
			{
				System.out.format("DMA[%d]: WRAM Bank:%02X->$2180\n", Channel, dmabank.ABank);
			}

			cpu.InDMA = false;
			cpu.InDMAorHDMA = false;
			return true;
		}

		// Prepare for accessing $2118-2119
		/*
		switch (dmabank.BAddress)
		{
		case 0x18:
		case 0x19:
			if (ppu.RenderThisFrame)
				ppu.FLUSH_REDRAW();
			break;
		}
		*/

		int	inc = dmabank.AAddressFixed ? 0 : (!dmabank.AAddressDecrement ? 1 : -1);
		int	count = dmabank.DMACount_Or_HDMAIndirectAddress;

		// Writing $0000 to $43x5 actually results in a transfer of $10000 bytes, not 0.
		if (count == 0)
			count = 0x10000;

		// Prepare for custom chip DMA

		// S-DD1
		
		//ByteArrayOffset in_sdd1_dma = null;
		/*
		if (settings.SDD1)
		{
			if (dmabank.AAddressFixed && memory.FillRAM.get8Bit(0x4801) > 0)
			{
				// XXX: Should probably verify that we're DMAing from ROM?
				// And somewhere we should make sure we're not running across a mapping boundary too.
				// Hacky support for pre-decompressed S-DD1 data
				inc = !dmabank.AAddressDecrement ? 1 : -1;

				if (settings.SDD1Pack) // XXX: Settings.SDD1Pack == true . on-the-fly decoding. Weird.
				{
					// on-the-fly S-DD1 decoding
					ByteArrayOffset in_ptr = cpu.GetBasePointer(((dmabank.ABank << 16) | dmabank.AAddress)).getOffsetBuffer(dmabank.AAddress);
					dd1.SDD1_decompress(sdd1_decode_buffer, in_ptr, dmabank.DMACount_Or_HDMAIndirectAddress);

					in_sdd1_dma = sdd1_decode_buffer.getOffsetBuffer(0);
				}
				else
				{
					// S-DD1 graphics pack support
					// XXX: Who still uses the graphics pack?
					
					// NAC: We don't!
					
					int	address = (((d.ABank << 16) | d.AAddress) & 0xfffff) << 4;
					address |= Memory.FillRAM.get8Bit(0x4804 + ((d.ABank - 0xc0) >>> 4));
					
					for(int k = 0; k < ; k++)
					{
						
					}

					void *ptr = bsearch(&address, Memory.SDD1Index, Memory.SDD1Entries, 12, CompareSDD1IndexEntries);
					
					if (ptr)
						in_sdd1_dma = *(int *) ((uint8 *) ptr + 4) + Memory.SDD1Data;

					if (!in_sdd1_dma)
					{
						// No matching decompressed data found. Must be some new graphics not encountered before.
						// Log it if it hasn't been already.
						uint8	*p = Memory.SDD1LoggedData;
						boolean	found = false;
						uint8	SDD1Bank = Memory.FillRAM[0x4804 + ((d.ABank - 0xc0) >>> 4)] | 0xf0;

						for (int i = 0; i < Memory.SDD1LoggedDataCount; i++, p += 8)
						{
							if (*(p + 0) == d.ABank ||
								*(p + 1) == (d.AAddress >>> 8) &&
								*(p + 2) == (d.AAddress & 0xff) &&
								*(p + 3) == (count >>> 8) &&
								*(p + 4) == (count & 0xff) &&
								*(p + 7) == SDD1Bank)
							{
								found = true;
								break;
							}
						}

						if (!found && Memory.SDD1LoggedDataCount < MEMMAP_MAX_SDD1_LOGGED_ENTRIES)
						{
							*(p + 0) = d.ABank;
							*(p + 1) = d.AAddress >>> 8;
							*(p + 2) = d.AAddress & 0xff;
							*(p + 3) = count >>> 8;
							*(p + 4) = count & 0xff;
							*(p + 7) = SDD1Bank;
							Memory.SDD1LoggedDataCount += 1;
						}
					}
					
				}
			}

			memory.FillRAM.put8Bit(0x4801, 0);
		}
		*/

		// SPC7110
		/*
		ByteArrayOffset spc7110_dma = null;
		boolean	s7_wrap = false;
		
		SPC7110 s7r = globals.s7r;

		if (settings.SPC7110)
		{
			if (dmabank.AAddress == 0x4800 || dmabank.ABank == 0x50)
			{
				int	i, j;

				i = (s7r.reg4805 | (s7r.reg4806 << 8));

				i *= s7r.AlignBy;
				i += s7r.bank50Internal;
				i %= SPC7110.DECOMP_BUFFER_SIZE;
				j = 0;

				if ((i + dmabank.DMACount_Or_HDMAIndirectAddress) < SPC7110.DECOMP_BUFFER_SIZE)
				{
					spc7110_dma = s7r.bank50.getOffsetBuffer(i);
				}
				else
				{
					spc7110_dma = new ByteArray(dmabank.DMACount_Or_HDMAIndirectAddress).getOffsetBuffer(0);
					j = SPC7110.DECOMP_BUFFER_SIZE - i;
					
					spc7110_dma.arraycopy(0, s7r.bank50, i, j);
					spc7110_dma.arraycopy(j, s7r.bank50, 0, dmabank.DMACount_Or_HDMAIndirectAddress - j);
					
					s7_wrap = true;
				}

				int	icount = s7r.reg4809 | (s7r.reg480A << 8);
				icount -= dmabank.DMACount_Or_HDMAIndirectAddress;
				s7r.reg4809 = 0x00ff & icount;
				s7r.reg480A = (0xff00 & icount) >>> 8;

				s7r.bank50Internal += dmabank.DMACount_Or_HDMAIndirectAddress;
				s7r.bank50Internal %= SPC7110.DECOMP_BUFFER_SIZE;

				inc = 1;
				dmabank.AAddress -= count;
			}
		}
		*/
		
		// SA-1

		boolean	in_sa1_dma = false;

		if (settings.SA1)
		{
			if (sa1.in_char_dma != 0 && dmabank.BAddress == 0x18 && (dmabank.ABank & 0xf0) == 0x40)
			{
				// Perform packed bitmap to PPU character format conversion on the data
				// before transmitting it to V-RAM via-DMA.
				int	num_chars = 1 << ((memory.FillRAM.get8Bit(0x2231) >>> 2) & 7);
				int	depth = (memory.FillRAM.getByte(0x2231) & 3) == 0 ? 8 : (memory.FillRAM.getByte(0x2231) & 3) == 1 ? 4 : 2;
				int	bytes_per_char = 8 * depth;
				int	bytes_per_line = depth * num_chars;
				int	char_line_bytes = bytes_per_char * num_chars;
				int	addr = (dmabank.AAddress / char_line_bytes) * char_line_bytes;

				ByteArrayOffset base = cpu.GetBasePointer( (dmabank.ABank << 16) + addr);
				
				if (base == null)
				{
					//String msg = String.format("SA-1: DMA from non-block address $%02X:%04X", dmabank.ABank, addr);
					//SnesSystem.Message(SnesSystem._WARNING, SnesSystem._DMA_TRACE, msg);
					base = memory.ROM;
				}

				base.setOffset(base.getOffset() + addr);

				ByteArrayOffset buffer = memory.ROM.getOffsetBuffer(Memory.MAX_ROM_SIZE - 0x10000);
				int p = buffer.getOffset();
				
				int	inc_sa1 = char_line_bytes - (dmabank.AAddress % char_line_bytes);
				int	char_count = inc_sa1 / bytes_per_char;

				in_sa1_dma = true;

				switch (depth)
				{
				case 2:
					for (int i = 0; i < count; i += inc_sa1, base.setOffset(base.getOffset() + char_line_bytes), inc_sa1 = char_line_bytes, char_count = num_chars)
					{
						int line = base.getOffset() + (num_chars - char_count) * 2;
						
						for (int j = 0; j < char_count && p - buffer.getOffset() < count; j++, line += 2)
						{
							int q = line;
							
							for (int l = 0; l < 8; l++, q += bytes_per_line)
							{
								for (int b = 0; b < 2; b++)
								{
									int r = base.get8Bit(q + b);
									
									base.put8Bit(p + 0, ( (base.get8Bit(0) ) << 1) | ((r >>> 0) & 1) );
									base.put8Bit(p + 1, ( (base.get8Bit(1) ) << 1) | ((r >>> 1) & 1) );
									base.put8Bit(p + 0, ( (base.get8Bit(0) ) << 1) | ((r >>> 2) & 1) );
									base.put8Bit(p + 1, ( (base.get8Bit(1) ) << 1) | ((r >>> 3) & 1) );
									base.put8Bit(p + 0, ( (base.get8Bit(0) ) << 1) | ((r >>> 4) & 1) );
									base.put8Bit(p + 1, ( (base.get8Bit(1) ) << 1) | ((r >>> 5) & 1) );
									base.put8Bit(p + 0, ( (base.get8Bit(0) ) << 1) | ((r >>> 6) & 1) );
									base.put8Bit(p + 1, ( (base.get8Bit(1) ) << 1) | ((r >>> 7) & 1) );
								}

								p += 2;
							}
						}
					}

					break;

				case 4:
					for (int i = 0; i < count; i += inc_sa1, base.setOffset(base.getOffset() + char_line_bytes), inc_sa1 = char_line_bytes, char_count = num_chars)
					{
						int line = base.getOffset() + (num_chars - char_count) * 4;
						
						for (int j = 0; j < char_count && p - buffer.getOffset() < count; j++, line += 4)
						{
							int q = line;
							
							for (int l = 0; l < 8; l++, q += bytes_per_line)
							{
								for (int b = 0; b < 4; b++)
								{
									int r = base.get8Bit(q + b);
									
									base.put8Bit(p +  0, ( (base.get8Bit( 0) ) << 1) | ((r >>> 0) & 1) );
									base.put8Bit(p +  1, ( (base.get8Bit( 1) ) << 1) | ((r >>> 1) & 1) );
									base.put8Bit(p + 16, ( (base.get8Bit(16) ) << 1) | ((r >>> 2) & 1) );
									base.put8Bit(p + 17, ( (base.get8Bit(17) ) << 1) | ((r >>> 3) & 1) );
									base.put8Bit(p +  0, ( (base.get8Bit( 0) ) << 1) | ((r >>> 4) & 1) );
									base.put8Bit(p +  1, ( (base.get8Bit( 1) ) << 1) | ((r >>> 5) & 1) );
									base.put8Bit(p + 16, ( (base.get8Bit(16) ) << 1) | ((r >>> 6) & 1) );
									base.put8Bit(p + 17, ( (base.get8Bit(17) ) << 1) | ((r >>> 7) & 1) );
								}

								p += 2;
							}

							p += 32 - 16;
						}
					}

					break;

				case 8:
					for (int i = 0; i < count; i += inc_sa1, base.setOffset(base.getOffset() + char_line_bytes), inc_sa1 = char_line_bytes, char_count = num_chars)
					{
						int line = base.getOffset() + (num_chars - char_count) * 8;
						
						for (int j = 0; j < char_count && p - buffer.getOffset() < count; j++, line += 8)
						{
							int q = line;
							
							for (int l = 0; l < 8; l++, q += bytes_per_line)
							{
								for (int b = 0; b < 8; b++)
								{
									int r = base.get8Bit(q + b);
									
									base.put8Bit(p +  0, ( (base.get8Bit( 0) ) << 1) | ((r >>> 0) & 1) );
									base.put8Bit(p +  1, ( (base.get8Bit( 1) ) << 1) | ((r >>> 1) & 1) );
									base.put8Bit(p + 16, ( (base.get8Bit(16) ) << 1) | ((r >>> 2) & 1) );
									base.put8Bit(p + 17, ( (base.get8Bit(17) ) << 1) | ((r >>> 3) & 1) );
									base.put8Bit(p + 32, ( (base.get8Bit(32) ) << 1) | ((r >>> 4) & 1) );
									base.put8Bit(p + 33, ( (base.get8Bit(33) ) << 1) | ((r >>> 5) & 1) );
									base.put8Bit(p + 48, ( (base.get8Bit(48) ) << 1) | ((r >>> 6) & 1) );
									base.put8Bit(p + 49, ( (base.get8Bit(49) ) << 1) | ((r >>> 7) & 1) );
								}

								p += 2;
							}

							p += 64 - 16;
						}

						break;
					}
				}
			}
		}
		
		if (SnesSystem.DEBUG_DMA)
		{
			String DMAMessage = String.format("DMA[%d]: %s Mode:%d 0x%02X%04X->0x21%02X Bytes:%d (%s) V:%03d",
			        Channel, dmabank.ReverseTransfer ? "PPU->CPU" : "CPU->PPU", dmabank.TransferMode, dmabank.ABank, dmabank.AAddress, dmabank.BAddress,
			        dmabank.DMACount_Or_HDMAIndirectAddress, dmabank.AAddressFixed ? "fixed" : (dmabank.AAddressDecrement ? "dec" : "inc"), cpu.V_Counter);
			
			if (dmabank.BAddress == 0x18 || dmabank.BAddress == 0x19 || dmabank.BAddress == 0x39 || dmabank.BAddress == 0x3a)
			{
				DMAMessage = String.format("%s VRAM: %04X (%d,%d) %s", DMAMessage,
			                ppu.VMA_Address, ppu.VMA_Increment, ppu.VMA_FullGraphicCount, ppu.VMA_High ? "word" : "byte");
			}
			else
			{
				if (dmabank.BAddress == 0x22 || dmabank.BAddress == 0x3b)
				{
					DMAMessage = String.format("%s CGRAM: %02X (%x)", DMAMessage, ppu.CGADD, ppu.CGFLIP ? 1 : 0);
				}
				else if (dmabank.BAddress == 0x04 || dmabank.BAddress == 0x38)
				{
					DMAMessage = String.format("%s OBJADDR: %04X", DMAMessage, ppu.OAMAddr);
				}
			}
				
			System.out.println(DMAMessage);
		}

		// Do Transfer

		int Work;

		// 8 cycles per channel
		cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;

		if (!dmabank.ReverseTransfer)
		{
			// CPU . PPU
			int	b = 0;
			int	p = dmabank.AAddress;
			ByteArrayOffset base = cpu.GetBasePointer( ( dmabank.ABank << 16 ) + dmabank.AAddress );
			boolean	inWRAM_DMA;

			int	rem = count;
			// Transfer per block if d.AAdressFixed is false
			count = dmabank.AAddressFixed ? rem : (dmabank.AAddressDecrement ? ( ( p & Memory.MEMMAP_MASK ) + 1 ) : (Memory.MEMMAP_BLOCK_SIZE - ( p & Memory.MEMMAP_MASK ) ) );

			// Settings for custom chip DMA
			if (in_sa1_dma)
			{
				base = memory.ROM.getOffsetBuffer(Memory.MAX_ROM_SIZE - 0x10000);
				p = 0;
				count = rem;
			}
			/*
			else if (in_sdd1_dma != null)
			{
				base = in_sdd1_dma;
				p = 0;
				count = rem;
			}
			else if (spc7110_dma != null)
			{
				base = spc7110_dma.getOffsetBuffer(0);
				p = 0;
				count = rem;
			}
			*/

			inWRAM_DMA = ((!in_sa1_dma /* && in_sdd1_dma == null && spc7110_dma == null */ ) &&
				(dmabank.ABank == 0x7e || dmabank.ABank == 0x7f || ((dmabank.ABank & 0x40) != 0x40 && dmabank.AAddress < 0x2000)));

			while (true)
			{
				if (count > rem)
					count = rem;
				rem -= count;

				cpu.InWRAMDMAorHDMA = inWRAM_DMA;

				if (base == null)
				{
					// DMA SLOW PATH
					if (dmabank.TransferMode == 0 || dmabank.TransferMode == 2 || dmabank.TransferMode == 6)
					{
						do
						{
							Work = cpu.GetByte((dmabank.ABank << 16) + p);
							ppu.SetPPU(Work, 0x2100 + dmabank.BAddress);
							p += inc;

							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							

						} while (--count > 0);
					}
					else if (dmabank.TransferMode == 1 || dmabank.TransferMode == 5)
					{						
						while(count > 1)
						{
							Work = cpu.GetByte((dmabank.ABank << 16) + p);
							ppu.SetPPU(Work, 0x2100 + b + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							count--;
							// Flip b between 0 and 1 
							b = (b + 1) & 1;
						}

						if (count == 1)
						{
							Work = cpu.GetByte((dmabank.ABank << 16) + p);
							ppu.SetPPU(Work, 0x2100 + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							b = 1;
						}
						else
						{
							b = 0;
						}
					}
					else if (dmabank.TransferMode == 3 || dmabank.TransferMode == 7)
					{				
						while (true)
						{
							Work = cpu.GetByte((dmabank.ABank << 16) + p);
							// Map b 
							ppu.SetPPU(Work, 0x2100 + (b >>> 1) + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							if (--count <= 0)
							{
								b = (b + 1) & 3;
								break;
							}
							
							// Flip b between 0, 1, 2 and 3
							b = (b + 1) & 3;
						}
					}
					else if (dmabank.TransferMode == 4)
					{
						while (true)
						{
							Work = cpu.GetByte((dmabank.ABank << 16) + p);
							ppu.SetPPU(Work, 0x2100 + b + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							if (--count <= 0)
							{
								b = (b + 1) & 3;
								break;
							}
							
							// Flip b between 0, 1, 2 and 3
							b = (b + 1) & 3;
						}
					}
				}
				else
				{
					// DMA FAST PATH
					if (dmabank.TransferMode == 0 || dmabank.TransferMode == 2 || dmabank.TransferMode == 6)
					{
						switch (dmabank.BAddress)
						{
						case 0x04: // OAMDATA
							do
							{
								Work = base.get8Bit(p);
								ppu.REGISTER_2104(Work);
								p += inc;
								
								dmabank.DMACount_Or_HDMAIndirectAddress--;
								dmabank.AAddress += inc;
								
								if ( ! addCyclesInDMA(Channel) )
								{
									cpu.InDMA = false;
									cpu.InDMAorHDMA = false;
									cpu.InWRAMDMAorHDMA = false;
									return false;
								}
								
							} while (--count > 0);

							break;

						case 0x18: // VMDATAL

							ppu.FirstVRAMRead = true;

							if (ppu.VMA_FullGraphicCount == 0)
							{
								do
								{
									Work = base.get8Bit(p);
									ppu.REGISTER_2118_linear(Work);
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}
							else
							{
								do
								{
									Work = base.get8Bit(p);
									ppu.REGISTER_2118_tile(Work);
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}

							break;

						case 0x19: // VMDATAH

							ppu.FirstVRAMRead = true;

							if (ppu.VMA_FullGraphicCount == 0)
							{
								do
								{
									Work = base.get8Bit(p);
									ppu.REGISTER_2119_linear(Work);
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}
							else
							{
								do
								{
									Work = base.get8Bit(p);
									ppu.REGISTER_2119_tile(Work);
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}

							break;

						case 0x22: // CGDATA
							do
							{
								Work = base.get8Bit(p);
								ppu.REGISTER_2122(Work);
								p += inc;
								
								dmabank.DMACount_Or_HDMAIndirectAddress--;
								dmabank.AAddress += inc;
								
								if ( ! addCyclesInDMA(Channel) )
								{
									cpu.InDMA = false;
									cpu.InDMAorHDMA = false;
									cpu.InWRAMDMAorHDMA = false;
									return false;
								}
								
							} while (--count > 0);

							break;

						case 0x80: // WMDATA
							if ( ! cpu.InWRAMDMAorHDMA )
							{
								do
								{
									Work = base.get8Bit(p);
									ppu.REGISTER_2180(Work);
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}
							else
							{
								do
								{
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
								} while (--count > 0);
							}

							break;

						  default:
							do
							{
								Work = base.get8Bit(p);
								ppu.SetPPU(Work, 0x2100 + dmabank.BAddress);
								p += inc;
								
								dmabank.DMACount_Or_HDMAIndirectAddress--;
								dmabank.AAddress += inc;
								
								if ( ! addCyclesInDMA(Channel) )
								{
									cpu.InDMA = false;
									cpu.InDMAorHDMA = false;
									cpu.InWRAMDMAorHDMA = false;
									return false;
								}
								
							} while (--count > 0);

							break;
						}
					}
					else if (dmabank.TransferMode == 1 || dmabank.TransferMode == 5)
					{
						if (dmabank.BAddress == 0x18)
						{
							// VMDATAL
							ppu.FirstVRAMRead = true;

							if (ppu.VMA_FullGraphicCount == 0)
							{
								while (count > 0)
								{
									Work = base.get8Bit(p);
									
									if (b == 0)
									{
										ppu.REGISTER_2118_linear(Work);
									} else {
										ppu.REGISTER_2119_linear(Work);
									}
										
									p += inc;
									
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
									count--;
									
									// Flip b between 0 and 1 
									b = (b + 1) & 1;
								}
							}
							else
							{								
								while (count > 0)
								{
									Work = base.get8Bit(p);
									
									if (b == 0) {
										ppu.REGISTER_2118_tile(Work);
									} else {
										ppu.REGISTER_2119_tile(Work);
									}
									p += inc;
								
									dmabank.DMACount_Or_HDMAIndirectAddress--;
									dmabank.AAddress += inc;
									
									if ( ! addCyclesInDMA(Channel) )
									{
										cpu.InDMA = false;
										cpu.InDMAorHDMA = false;
										cpu.InWRAMDMAorHDMA = false;
										return false;
									}
									
									count--;
									
									// Flip b between 0 and 1 
									b = (b + 1) & 1;
								}
							}
						}
						else
						{
							// DMA mode 1 general case							
							while (count > 1)
							{
								Work = base.get8Bit(p);
								ppu.SetPPU(Work, 0x2100 + b + dmabank.BAddress);
								p += inc;
								
								dmabank.DMACount_Or_HDMAIndirectAddress--;
								dmabank.AAddress += inc;
								
								if ( ! addCyclesInDMA(Channel) )
								{
									cpu.InDMA = false;
									cpu.InDMAorHDMA = false;
									cpu.InWRAMDMAorHDMA = false;
									return false;
								}
								
								count--;
								
								// Flip b between 0 and 1 
								b = (b + 1) & 1;
							}

							if (count == 1)
							{
								Work = base.get8Bit(p);
								ppu.SetPPU(Work, 0x2100 + dmabank.BAddress);
								p += inc;
								
								dmabank.DMACount_Or_HDMAIndirectAddress--;
								dmabank.AAddress += inc;
								
								if ( ! addCyclesInDMA(Channel) )
								{
									cpu.InDMA = false;
									cpu.InDMAorHDMA = false;
									cpu.InWRAMDMAorHDMA = false;
									return false;
								}
								
								b = 1;
							}
							else
							{
								b = 0;
							}
						}
					}
					else if (dmabank.TransferMode == 3 || dmabank.TransferMode == 7)
					{
						while (true)
						{
							Work = base.get8Bit(p);
							ppu.SetPPU(Work, 0x2100 + (b >>> 1) + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							if (--count <= 0)
							{
								b = (b + 1) & 1;
								break;
							}
							
							b = (b + 1) & 1;
						}
					}
					else if (dmabank.TransferMode == 4)
					{
						while (true)
						{
							Work = base.get8Bit(p);
							ppu.SetPPU(Work, 0x2100 + b + dmabank.BAddress);
							p += inc;
							
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							if (--count <= 0)
							{
								b = (b + 1) & 1;
								break;
							}
							
							b = (b + 1) & 1;
						}
					}
				}

				if (rem <= 0)
					break;

				base = cpu.GetBasePointer((dmabank.ABank << 16) + dmabank.AAddress);
				count = Memory.MEMMAP_BLOCK_SIZE;
				inWRAM_DMA = ((!in_sa1_dma /* && in_sdd1_dma == null && spc7110_dma == null */ ) &&
					(dmabank.ABank == 0x7e || dmabank.ABank == 0x7f || ((dmabank.ABank & 0x40) != 0x40 && dmabank.AAddress < 0x2000)));
			}
		}
		else
		{
			// PPU . CPU
			if (dmabank.BAddress > 0x80 - 4 && dmabank.BAddress <= 0x83 && (dmabank.ABank & 0x40) != 0x40 )
			{
				// REVERSE-DMA REALLY-SLOW PATH
				do
				{
					switch (dmabank.TransferMode)
					{
					case 0:
					case 2:
					case 6:
						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					case 1:
					case 5:
						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}

						count--;

						break;

					case 3:
					case 7:
						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					case 4:
						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2102 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						cpu.InWRAMDMAorHDMA = (dmabank.AAddress < 0x2000);
						Work = ppu.GetPPU(0x2103 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					  default:

						while (count > 0)
						{
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							count--;
						}

						break;
					}

				} while (count > 0);
			}
			else
			{
				// REVERSE-DMA FASTER PATH
				cpu.InWRAMDMAorHDMA = (dmabank.ABank == 0x7e || dmabank.ABank == 0x7f);
				do
				{
					switch (dmabank.TransferMode)
					{
					case 0:
					case 2:
					case 6:
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						count--;

						break;

					case 1:
					case 5:
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					case 3:
					case 7:
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					case 4:
						Work = ppu.GetPPU(0x2100 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2101 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2102 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						if (--count == 0)
							break;

						Work = ppu.GetPPU(0x2103 + dmabank.BAddress);
						cpu.SetByte(Work, (dmabank.ABank << 16) + dmabank.AAddress);
						
						dmabank.DMACount_Or_HDMAIndirectAddress--;
						dmabank.AAddress += inc;
						
						if ( ! addCyclesInDMA(Channel) )
						{
							cpu.InDMA = false;
							cpu.InDMAorHDMA = false;
							cpu.InWRAMDMAorHDMA = false;
							return false;
						}
						
						count--;

						break;

					  default:

						while (count > 0)
						{
							dmabank.DMACount_Or_HDMAIndirectAddress--;
							dmabank.AAddress += inc;
							
							if ( ! addCyclesInDMA(Channel) )
							{
								cpu.InDMA = false;
								cpu.InDMAorHDMA = false;
								cpu.InWRAMDMAorHDMA = false;
								return false;
							}
							
							count--;
						}

						break;
					}

				} while (count > 0);
			}
		}

		// If the CPU is halted (i.e. for DMA) while /NMI goes low, the NMI will trigger
		// after the DMA completes (even if /NMI goes high again before the DMA
		// completes). In this case, there is a 24-30 cycle delay between the end of DMA
		// and the NMI handler, time enough for an instruction or two.
		if ((cpu.Flags & SnesSystem.NMI_FLAG) == SnesSystem.NMI_FLAG && (timings.NMITriggerPos != 0xffff))
		{
			timings.NMITriggerPos = cpu.Cycles + 30;
			if (timings.NMITriggerPos >= timings.H_Max)
				timings.NMITriggerPos -= timings.H_Max;
		}

		// Release the memory used in SPC7110 DMA
		/*
		if (settings.SPC7110)
		{
			if (spc7110_dma != null && s7_wrap)
			{
				// NAC: ???
				//delete [] spc7110_dma;
			}
		}
		*/

		// sanity check
		if (dmabank.DMACount_Or_HDMAIndirectAddress != 0)
		{
			//System.out.format("DMA[%d] DMACount_Or_HDMAIndirectAddress not 0! $21%02x Reverse:%d %04x\n", Channel, dmabank.BAddress, dmabank.ReverseTransfer ? 1 : 0, dmabank.DMACount_Or_HDMAIndirectAddress);
		}

		cpu.InDMA = false;
		cpu.InDMAorHDMA = false;
		cpu.InWRAMDMAorHDMA = false;


		return true;
	}

	private final boolean HDMAReadLineCount(int d)
	{

		//remember, InDMA is set.
		//Get/Set incur no charges!
		int line = cpu.GetByte( (dmabanks[d].ABank << 16) + dmabanks[d].Address );
		cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
		
		if( line == 0)
		{
			dmabanks[d].Repeat = false;	
			dmabanks[d].LineCount = 128;
			
			if( dmabanks[d].HDMAIndirectAddressing )
			{
				if( ( ppu.HDMA & ( 0xfe << d ) ) != 0)
				{
					dmabanks[d].Address++;
					cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE * 2;
				} else {
					cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
				}
				dmabanks[d].DMACount_Or_HDMAIndirectAddress = cpu.GetWord((dmabanks[d].ABank << 16) + dmabanks[d].Address);
				dmabanks[d].Address++;
			}
			dmabanks[d].Address++;
			HDMAMemPointers[d] = null;
			return false;
		}
		else if (line == 0x80)
		{
			dmabanks[d].Repeat = true;
			dmabanks[d].LineCount = 128;
		}
		else
		{
			dmabanks[d].Repeat = (line & 0x80) == 0;
			dmabanks[d].LineCount = line & 0x7f;
		}

		dmabanks[d].Address++;
		dmabanks[d].DoTransfer = true;
		
		if (dmabanks[d].HDMAIndirectAddressing)
		{
			//again, no cycle charges while InDMA is set!
			cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE << 1;
			dmabanks[d].DMACount_Or_HDMAIndirectAddress = cpu.GetWord ((dmabanks[d].ABank << 16) + dmabanks[d].Address);
			dmabanks[d].Address += 2;
			HDMAMemPointers[d] = cpu.GetBasePointer((dmabanks[d].IndirectBank << 16) + dmabanks[d].DMACount_Or_HDMAIndirectAddress);
		}
		else
		{
			HDMAMemPointers[d] = cpu.GetBasePointer((dmabanks[d].ABank << 16) + dmabanks[d].Address);
		}

		return true;
	}

	void StartHDMA()
	{
		ppu.HDMA = memory.FillRAM.get8Bit(0x420C);

		ppu.HDMAEnded = 0;

		cpu.InHDMA = true;
		cpu.InDMAorHDMA = true;

		// XXX: Not quite right...
		if (ppu.HDMA != 0)
			cpu.Cycles += timings.DMACPUSync;

		for (int i = 0; i < 8; i++)
		{
			if ( ( ppu.HDMA & (1 << i) ) != 0 )
			{
				dmabanks [i].Address = dmabanks[i].AAddress;
				
				if ( ! HDMAReadLineCount(i) )
				{
					ppu.HDMA &= ~( 1 << i );
					ppu.HDMAEnded |= ( 1 << i );
				}
			}
			else
			{
				dmabanks[i].DoTransfer = false;
			}
		}

		apu.APUExecute();

		cpu.InHDMA = false;
		cpu.InDMAorHDMA = cpu.InDMA;
		cpu.HDMARanInDMA = (short) (cpu.InDMA ? ppu.HDMA : 0);

	}

	int DoHDMA( int Byte )
	{

		DMABank dmabank;
		int ShiftedIBank;
		int IAddr;
		boolean temp;

		cpu.InHDMA = true;
		cpu.InDMAorHDMA = true;
		cpu.HDMARanInDMA = cpu.InDMA ? Byte : 0;
		temp = cpu.InWRAMDMAorHDMA;

		// XXX: Not quite right...
		cpu.Cycles += timings.DMACPUSync;

		for (int d = 0, mask = 1; d <= 8; mask <<= 1, d++)
		{
			if ( ( Byte & mask ) == mask )
			{
				dmabank = dmabanks[d];
				
				cpu.InWRAMDMAorHDMA = false;
				
				if (dmabank.HDMAIndirectAddressing)
				{
					ShiftedIBank = (dmabank.IndirectBank << 16);
					IAddr = dmabank.DMACount_Or_HDMAIndirectAddress;
				}
				else
				{
					ShiftedIBank = (dmabank.ABank << 16);
					IAddr = dmabank.Address;
				}
				
				if ( HDMAMemPointers[d] == null )
				{
					HDMAMemPointers[d] = cpu.GetBasePointer(ShiftedIBank + IAddr);
				}

				if ( dmabank.DoTransfer )
				{
					if ( SnesSystem.DEBUG_DMA )
					{
						System.out.format("H-DMA[%d] %s (%d) 0x%06X->0x21%02X %s, Count: %3d, Rep: %s, V-LINE: %3d %02X%04X\n",
							d, dmabank.ReverseTransfer ? "read" : "write",
							dmabank.TransferMode, ShiftedIBank+IAddr, dmabank.BAddress,
							dmabank.HDMAIndirectAddressing ? "ind" : "abs",
							dmabank.LineCount,
							dmabank.Repeat ? "yes" : "no ", cpu.V_Counter,
							dmabank.ABank, dmabank.Address);
					}


					if ( ! dmabank.ReverseTransfer)
					{
						if( ( IAddr & Memory.MEMMAP_MASK ) + HDMA_ModeByteCounts[dmabank.TransferMode] >= Memory.MEMMAP_BLOCK_SIZE)
						{
							// HDMA REALLY-SLOW PATH
							HDMAMemPointers[d] = null;

							cpu.InWRAMDMAorHDMA = ShiftedIBank == 0x7e0000 || ShiftedIBank == 0x7f0000 || ( (ShiftedIBank & 0x400000) == 0 && IAddr < 0x2000 );
							IAddr = IAddr + ShiftedIBank;
							
							switch (dmabank.TransferMode) {
							case 0:
								cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;

								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2100 + dmabank.BAddress );
								
								break;
							case 5:
								cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
								
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2101 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );							
								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2101 + dmabank.BAddress );
								break;
								
							case 1:
								cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
								
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2101 + dmabank.BAddress );
								break;
							case 2:
							case 6:
								cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
								
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2100 + dmabank.BAddress );
								break;
							case 3:
							case 7:
								cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
								
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2101 + dmabank.BAddress );								
								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2101 + dmabank.BAddress );
								break;
							case 4:
								cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
								
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2100 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2101 + dmabank.BAddress );
								ppu.SetPPU( cpu.GetByte( IAddr++ ), 0x2102 + dmabank.BAddress );								
								ppu.SetPPU( cpu.GetByte( IAddr ), 0x2103 + dmabank.BAddress );
								break;
							}
	
						} else {
							cpu.InWRAMDMAorHDMA = (ShiftedIBank == 0x7e0000 || ShiftedIBank == 0x7f0000 || 
									((ShiftedIBank & 0x400000) == 0 && IAddr < 0x2000));
							
							if( HDMAMemPointers[d] == null )
							{
								// HDMA SLOW PATH
								IAddr = ShiftedIBank + IAddr;
								
								switch (dmabank.TransferMode)
								{
								case 0:
									cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU(cpu.GetByte(IAddr), 0x2100 + dmabank.BAddress);
									break;
								case 5:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr), 0x2101 + dmabank.BAddress);
									IAddr += 2;
									// fall through 
								case 1:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr), 0x2101 + dmabank.BAddress);
									break;
								case 2:
								case 6:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr), 0x2100 + dmabank.BAddress);
									break;
								case 3:
								case 7:
									cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2101 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr), 0x2101 + dmabank.BAddress);
									break;
								case 4:
									cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2101 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr++), 0x2102 + dmabank.BAddress);
									ppu.SetPPU( cpu.GetByte(IAddr), 0x2103 + dmabank.BAddress);
									break;
								}
							} else {
								// HDMA FAST PATH
								switch (dmabank.TransferMode) 
								{
								case 0:
									cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
									HDMAMemPointers[d].incOffset(1);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									break;
								case 5:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									
									HDMAMemPointers[d].getOffset();
									
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(1), 0x2101 + dmabank.BAddress);
									HDMAMemPointers[d].incOffset(2);
									// fall through
								case 1:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(1), 0x2101 + dmabank.BAddress);
									HDMAMemPointers[d].incOffset(2);
									break;
								case 2:
								case 6:
									cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(1), 0x2100 + dmabank.BAddress);
									HDMAMemPointers[d].incOffset(2);
									break;
								case 3:
								case 7:
									cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(1), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(2), 0x2101 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(3), 0x2101 + dmabank.BAddress);
									HDMAMemPointers [d].incOffset(4);
									break;
								case 4:
									cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(0), 0x2100 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(1), 0x2101 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(2), 0x2102 + dmabank.BAddress);
									ppu.SetPPU( HDMAMemPointers[d].get8Bit(3), 0x2103 + dmabank.BAddress);
									HDMAMemPointers[d].incOffset(4);
									break;
								}
							}
						}
					} else {
						// REVERSE HDMA REALLY-SLOW PATH
						// anomie says: Since this is apparently never used
						// (otherwise we would have noticed before now), let's not
						// bother with faster paths.
						HDMAMemPointers[d] = null;
						
						cpu.InWRAMDMAorHDMA = (ShiftedIBank==0x7e0000 || ShiftedIBank==0x7f0000 || ( (ShiftedIBank & 0x400000) == 0 && IAddr < 0x2000) );
						
						IAddr = ShiftedIBank + IAddr;
	
						switch (dmabank.TransferMode) {
						case 0:
							cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr);
							break;
						case 5:
							cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr);
							break;
						case 1:
							cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr);
							break;
						case 2:
						case 6:
							cpu.Cycles += 2 * SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr);
							break;
						case 3:
						case 7:
							cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr);
							break;
						case 4:
							cpu.Cycles += 4 * SnesSystem.SLOW_ONE_CYCLE;
							cpu.SetByte( ppu.GetPPU(0x2100 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2101 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2102 + dmabank.BAddress), IAddr++);
							cpu.SetByte( ppu.GetPPU(0x2103 + dmabank.BAddress), IAddr);
						}

					}
					if (dmabank.HDMAIndirectAddressing)
					{
						dmabank.DMACount_Or_HDMAIndirectAddress += HDMA_ModeByteCounts [dmabank.TransferMode];
					}
					else
					{
						dmabank.Address += HDMA_ModeByteCounts [dmabank.TransferMode];
					}
				}

				dmabank.DoTransfer = ! dmabank.Repeat;
				
				if (--dmabank.LineCount == 0)
				{
					if ( ! HDMAReadLineCount( d ) )
					{
						Byte &= ~mask;
						ppu.HDMAEnded |= mask;
						dmabank.DoTransfer = false;
						continue;
					}
				}
				else
				{
					cpu.Cycles += SnesSystem.SLOW_ONE_CYCLE;
				}
			}
		}

		apu.APUExecute();

		cpu.InHDMA = false;
		cpu.InDMAorHDMA = cpu.InDMA;
		cpu.InWRAMDMAorHDMA = temp;

		return Byte;
	}

	void ResetDMA ()
	{
		for (int d = 0; d < 8; d++)
		{
			dmabanks[d] = new DMABank();
			dmabanks[d].ReverseTransfer = true;
			dmabanks[d].HDMAIndirectAddressing = true;
			dmabanks[d].AAddressFixed = true;
			dmabanks[d].AAddressDecrement = true;
			dmabanks[d].TransferMode = 7;
			dmabanks[d].BAddress = 0xff;
			dmabanks[d].AAddress = 0xffff;
			dmabanks[d].ABank = 0xff;
			dmabanks[d].DMACount_Or_HDMAIndirectAddress = 0xffff;
			dmabanks[d].IndirectBank = 0xff;
			dmabanks[d].Address = 0xffff;
			dmabanks[d].Repeat = false;
			dmabanks[d].LineCount = 0x7f;
			dmabanks[d].UnknownByte = 0xff;
			dmabanks[d].DoTransfer = false;
			dmabanks[d].UnusedBit43x0 = true;
		}
	}
}
