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


final class PPU
{
	static final int FIRST_VISIBLE_LINE = 1;
	
	static final int TILE_2BIT = 0;
	static final int TILE_4BIT = 1;
	static final int TILE_8BIT = 2;
	//private static final int TILE_2BIT_EVEN = 3;
	//private static final int TILE_2BIT_ODD = 4;
	//private static final int TILE_4BIT_EVEN = 5;
	//private static final int TILE_4BIT_ODD = 6;
	
	private static final int CLIP_OR = 0;
	private static final int CLIP_AND = 1;
	private static final int CLIP_XOR = 2;
	private static final int CLIP_XNOR = 3;

	static final int MAX_2BIT_TILES = 4096;
	static final int MAX_4BIT_TILES = 2048;
	static final int MAX_8BIT_TILES = 1024;
		
	static final int PPU_H_BEAM_IRQ_SOURCE = (1 << 0);
	static final int PPU_V_BEAM_IRQ_SOURCE = (1 << 1);
	static final int GSU_IRQ_SOURCE        = (1 << 2);
	static final int SA1_IRQ_SOURCE        = (1 << 7);
	static final int SA1_DMA_IRQ_SOURCE    = (1 << 5);

	class ClipData
	{
		private int Count;
		private int[] DrawMode = new int[6];
		private int[] Left = new int[6];
		private int[] Right = new int[6];
		
		private void zero()
		{
			Count = 0;
			for (int i = 0; i < 6; i++)
			{
				DrawMode[i] = 0;
				Left[i] = 0;
				Right[i] = 0;
			}
		}
	}

	boolean ColorsChanged;
    int HDMA;
    int HDMAEnded;

    boolean OBJChanged;
    boolean RenderThisFrame;

    int FrameCount;
    private int RenderedFramesCount;
    int TotalEmulatedFrames;


    //private byte[][] TileCache = new byte[7][];
    
    byte[][] TileCached = new byte[3][];
    
    byte[] PaletteCached = new byte[16];

    private int VRAMReadBuffer;
    boolean Interlace;
    private boolean InterlaceOBJ;
    private boolean PseudoHires;
    private boolean DoubleWidthPixels;
    private boolean DoubleHeightPixels;
    private int RenderedScreenHeight;
    private int RenderedScreenWidth;
    
    private int PreviousLine;
    private int CurrentLine;
    
    private ClipData[][] Clip = new ClipData[2][6];
    
    boolean FirstVRAMRead;
    
    private String BGTileDimensions[] = {"32x32", "64x32", "32x64", "64x64"};
    
    int BGMode;
	boolean BG3Priority;
	int Brightness;
		
	private static final int BGCount = 4;
	
	Bg[] BG = new Bg[BGCount];
	
	//short BGScanLinePositions[][] = new short[BGCount][256 * 3];
	
	final static int BGIND_SCAN_LINE = 0;
	final static int BGIND_HORIZONTAL = 1;
	final static int BGIND_VERTICAL = 2;

	boolean CGFLIP;
	short[] CGDATA = new short[256];
	int FirstSprite;
	private int LastSprite;
	
	static final int OAMCount = 128;
	SOBJ[] OBJ = new SOBJ[OAMCount];
	
	boolean OAMPriorityRotation;
	int OAMAddr;
	int RangeTimeOver;
	
	int OAMFlip;
	int OAMTileAddress;
	private int IRQVBeamPos;
	private int IRQHBeamPos;
	private int VBeamPosLatched;
	private int HBeamPosLatched;
	
	private int HBeamFlip;
	private int VBeamFlip;
	int HVBeamCounterLatched;
	
	private int MatrixA;
	private int MatrixB;
	private int MatrixC;
	private int MatrixD;
	private int CentreX;
	private int CentreY;
	private int M7HOFS;
	private int M7VOFS;
	
	int CGADD;
	int FixedColourRed;
	int FixedColourGreen;
	int FixedColourBlue;
	int SavedOAMAddr;
	int ScreenHeight;
	private int WRAM;
	boolean ForcedBlanking;
	private boolean OBJThroughMain;
	private boolean OBJThroughSub;
	
	int OBJSizeSelect;
	int OBJNameBase;
	private boolean OBJAddition;
	private int OAMReadFlip;
	private ByteArray OAMData = new ByteArray(512 + 32);
	
	boolean VTimerEnabled;
	boolean HTimerEnabled;
	int  HTimerPosition;
	private int Mosaic;
	private int MosaicStart;
	private boolean[] BGMosaic = new boolean[4];
	private boolean Mode7HFlip;
	private boolean Mode7VFlip;
	private int Mode7Repeat;
	private int Window1Left;
	private int Window1Right;
	private int Window2Left;
	private int Window2Right;
	private byte[] ClipCounts = new byte[6];
	private int[] ClipWindowOverlapLogic = new int[6];
	private boolean[] ClipWindow1Enable = new boolean[6];
	private boolean[] ClipWindow2Enable = new boolean[6];
	private boolean[] ClipWindow1Inside = new boolean[6];
	private boolean[] ClipWindow2Inside = new boolean[6];
	private boolean RecomputeClipWindows;
	private int CGFLIPRead;
	private int OBJNameSelect;
	private boolean Need16x8Mulitply;
	
	private int OAMWriteRegister;
	private int BGnxOFSbyte;
	private int M7byte;
	private int OpenBus1;
	private int OpenBus2;
	int VTimerPosition;
	
	boolean VMA_High;
	int VMA_Increment;
	int VMA_Address;
	int VMA_Mask1;
	int VMA_FullGraphicCount;
	int VMA_Shift;
	
	private int random;
	
    PPU()
    {
    	// Allocate memory
    	/*
    	TileCache[TILE_2BIT]		= new byte[MAX_2BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_4BIT]		= new byte[MAX_4BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_8BIT]		= new byte[MAX_8BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_2BIT_EVEN]	= new byte[MAX_2BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_2BIT_ODD]	= new byte[MAX_2BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_4BIT_EVEN]	= new byte[MAX_4BIT_TILES * MAX_TILE_DIMENSION];
    	TileCache[TILE_4BIT_ODD]	= new byte[MAX_4BIT_TILES * MAX_TILE_DIMENSION];
    	*/

    	TileCached[TILE_2BIT]		= new byte[MAX_2BIT_TILES];
    	TileCached[TILE_4BIT]		= new byte[MAX_4BIT_TILES];
    	TileCached[TILE_8BIT]		= new byte[MAX_8BIT_TILES];
    	
    	//TileCached[TILE_2BIT_EVEN]	= new byte[MAX_2BIT_TILES];
    	//TileCached[TILE_2BIT_ODD]	= new byte[MAX_2BIT_TILES];
    	//TileCached[TILE_4BIT_EVEN]	= new byte[MAX_4BIT_TILES];
    	//TileCached[TILE_4BIT_ODD]	= new byte[MAX_4BIT_TILES];

    }
    
    void ZeroTileCache()
    {
        for( int i = 0; i < TileCached.length; i++)
        {
        	byte[] tmp = TileCached[i];
        	
        	for( int j = 0 ; j < tmp.length; j++)
        	{
        		//TileCache[i][j] = 0;
        		TileCached[i][j] = 0;
        	}
        }
    }
	
	class SOBJ
	{
		int HPos;
		int VPos;
		int Name;
		byte VH_Flip;
		byte Priority;
		int Palette;
		int Size;
		boolean Changed;
	}
	
	class Bg
	{
		int TileMapAddress; // Address of the tile maps
		short VOffset;
		short HOffset;
		int TileSizeMode; 
		int TileDataBase; // Address of the tile pixels
		int TileMapSize;
	};

	private static Globals globals;
	private Memory memory;
	private CPU cpu;
	private Timings timings;
	private APU apu;
	private Settings settings;
	private DMA dma;
	private ByteArray HDMAMemPointers;
	private SA1 sa1;
	private SuperFX superfx;
	private GLDisplay GameCanvas;
	
	void setUp()
	{
		globals = Globals.globals;
		memory = globals.memory;
		cpu = globals.cpu;
		timings = globals.timings;
		apu = globals.apu;
		settings = globals.settings;
		dma = globals.dma;
		sa1 = globals.sa1;
		superfx = globals.superfx;
		GameCanvas = globals.gamedisplay;
	}
	
	private void LatchCounters (boolean force)
	{
	    if ( force || (memory.FillRAM.getByte(0x4213) & 0x80) != 0 )
	    {
	        // Latch h and v counters, like the gun
	        HVBeamCounterLatched = 1;
	        VBeamPosLatched = cpu.V_Counter;
	
			// From byuu:
			// All dots are 4 cycles long, except dots 322 and 326. dots 322 and 326 are 6 cycles long.
			// This holds true for all scanlines except scanline 240 on non-interlace odd frames.
			// The reason for this is because this scanline is only 1360 cycles long,
			// instead of 1364 like all other scanlines.
			// This makes the effective range of hscan_pos 0-339 at all times.
			int hc = cpu.Cycles;
	
			if (timings.H_Max == timings.H_Max_Master)	// 1364
			{
				if (hc >= 1292)
					hc -= (SnesSystem.ONE_DOT_CYCLE / 2);
				if (hc >= 1308)
					hc -= (SnesSystem.ONE_DOT_CYCLE / 2);
			}
	
			HBeamPosLatched = hc / SnesSystem.ONE_DOT_CYCLE;
	
	        memory.FillRAM.put8Bit(0x213F, memory.FillRAM.getByte(0x213F) | 0x40);
	
	    }
	}

	void CheckMissingHTimerPosition (int hc)
	{		
		if (HTimerPosition == hc)
		{
			if (HTimerEnabled && (!VTimerEnabled || (cpu.V_Counter == VTimerPosition)))
			{
				cpu.SetIRQ(PPU.PPU_H_BEAM_IRQ_SOURCE);
			}
			else if (VTimerEnabled && (cpu.V_Counter == VTimerPosition))
			{
				cpu.SetIRQ(PPU.PPU_V_BEAM_IRQ_SOURCE);
			}
		}
	}

	void CheckMissingHTimerHalt (int hc_from, int range)
	{		
		if ((HTimerPosition >= hc_from) && (HTimerPosition < (hc_from + range)))
		{
			if (HTimerEnabled && (!VTimerEnabled || (cpu.V_Counter == VTimerPosition)))
			{
				cpu.IRQPending = 1;
			}
			else if (VTimerEnabled && (cpu.V_Counter == VTimerPosition))
			{
				cpu.IRQPending = 1;
			}
		}
	}

	private void CheckMissingHTimerRange (int hc_from, int range)
	{		
		if ((HTimerPosition >= hc_from) && (HTimerPosition < (hc_from + range)))
		{
			if (HTimerEnabled && (!VTimerEnabled || (cpu.V_Counter == VTimerPosition)))
			{
				cpu.SetIRQ(PPU.PPU_H_BEAM_IRQ_SOURCE);
			}
			else if (VTimerEnabled && (cpu.V_Counter == VTimerPosition))
			{
				cpu.SetIRQ(PPU.PPU_V_BEAM_IRQ_SOURCE);
			}
		}
	}
	
	private void UpdateHVTimerPosition()
	{
		if (HTimerEnabled)
		{
			if (IRQHBeamPos != 0)
			{
				// IRQ_read
				HTimerPosition = (int) (IRQHBeamPos * SnesSystem.ONE_DOT_CYCLE);
				if (timings.H_Max == timings.H_Max_Master)	// 1364
				{
					if (IRQHBeamPos > 322)
						HTimerPosition += (SnesSystem.ONE_DOT_CYCLE / 2);
					if (IRQHBeamPos > 326)
						HTimerPosition += (SnesSystem.ONE_DOT_CYCLE / 2);
				}
				HTimerPosition += 14;
				// /IRQ
				HTimerPosition += 4;
				// after CPU executing
				HTimerPosition += 6;
			}
			else
				HTimerPosition = 10 + 4 + 6;
		}
		else
			HTimerPosition = 10 + 4 + 6;

		VTimerPosition = IRQVBeamPos;

		if ((HTimerPosition >= timings.H_Max) && (IRQHBeamPos < 340))
		{
			HTimerPosition -= timings.H_Max;
			VTimerPosition++;
			// FIXME
			if (VTimerPosition >= timings.V_Max)
				VTimerPosition = 0;
		}

		if (HTimerPosition < cpu.Cycles)
		{
			switch (cpu.WhichEvent)
			{
			case SnesSystem.HC_IRQ_1_3_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HDMA_START_EVENT;
					cpu.NextEvent  = timings.HDMAStart;
					break;

			case SnesSystem.HC_IRQ_3_5_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HCOUNTER_MAX_EVENT;
					cpu.NextEvent  = timings.H_Max;
					break;

			case SnesSystem.HC_IRQ_5_7_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HDMA_INIT_EVENT;
					cpu.NextEvent  = timings.HDMAInit;
					break;

			case SnesSystem.HC_IRQ_7_9_EVENT:
					cpu.WhichEvent = SnesSystem.HC_RENDER_EVENT;
					cpu.NextEvent  = timings.RenderPos;
					break;

			case SnesSystem.HC_IRQ_9_A_EVENT:
					cpu.WhichEvent = SnesSystem.HC_WRAM_REFRESH_EVENT;
					cpu.NextEvent  = timings.WRAMRefreshPos;
					break;

			case SnesSystem.HC_IRQ_A_1_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HBLANK_START_EVENT;
					cpu.NextEvent  = timings.HBlankStart;
					break;
			}
		}
		else if ( (HTimerPosition < cpu.NextEvent) || 
			 ( ! ( ( cpu.WhichEvent & 1 ) > 0 ) && ( HTimerPosition == cpu.NextEvent ) ) )
		{
			cpu.NextEvent = HTimerPosition;

			switch (cpu.WhichEvent)
			{
			case SnesSystem.HC_HDMA_START_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_1_3_EVENT;
					break;

			case SnesSystem.HC_HCOUNTER_MAX_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_3_5_EVENT;
					break;

			case SnesSystem.HC_HDMA_INIT_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_5_7_EVENT;
					break;

			case SnesSystem.HC_RENDER_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_7_9_EVENT;
					break;

			case SnesSystem.HC_WRAM_REFRESH_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_9_A_EVENT;
					break;

			case SnesSystem.HC_HBLANK_START_EVENT:
					cpu.WhichEvent = SnesSystem.HC_IRQ_A_1_EVENT;
					break;
			}
		}
		else
		{
			switch (cpu.WhichEvent)
			{
			case SnesSystem.HC_IRQ_1_3_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HDMA_START_EVENT;
					cpu.NextEvent  = timings.HDMAStart;
					break;

			case SnesSystem.HC_IRQ_3_5_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HCOUNTER_MAX_EVENT;
					cpu.NextEvent  = timings.H_Max;
					break;

			case SnesSystem.HC_IRQ_5_7_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HDMA_INIT_EVENT;
					cpu.NextEvent  = timings.HDMAInit;
					break;

			case SnesSystem.HC_IRQ_7_9_EVENT:
					cpu.WhichEvent = SnesSystem.HC_RENDER_EVENT;
					cpu.NextEvent  = timings.RenderPos;
					break;

			case SnesSystem.HC_IRQ_9_A_EVENT:
					cpu.WhichEvent = SnesSystem.HC_WRAM_REFRESH_EVENT;
					cpu.NextEvent  = timings.WRAMRefreshPos;
					break;

			case SnesSystem.HC_IRQ_A_1_EVENT:
					cpu.WhichEvent = SnesSystem.HC_HBLANK_START_EVENT;
					cpu.NextEvent  = timings.HBlankStart;
					break;
			}
		}
	}
	
	void SetPPU( int Byte, int Address )
	{
		if ( SnesSystem.DEBUG_PPU )
		{
			//System.out.println( String.format("PPU SetByte: %02X to %04X", Byte, Address ));
		}
		
		// Take care of DMA wrapping
		if( cpu.InDMAorHDMA && Address > 0x21ff )
		{
			Address = 0x2100 + ( Address & 0xff );
		}

		if (Address <= 0x219F)
		{
			switch (Address)
			{
			case 0x2100:
				// Brightness and screen blank bit
				if (Byte != memory.FillRAM.get8Bit(0x2100) )
				{
					FLUSH_REDRAW ();
					
			    	// Calculate 8 bit brightness from 4 bits
					Brightness = ( ( ( ( Byte & 0xF ) + 1 ) << 4) - 1);
					
					BackgroundChanged();
					
					if ( SnesSystem.DEBUG_PPU_MODES && memory.FillRAM.get8Bit(Address) != Byte )
					{
						System.out.format("Brightness = %d\n", Brightness );
					}
					
					ForcedBlanking = ( Byte & 0x80 ) != 0;
				}
				
				if ( ( ( memory.FillRAM.getByte(0x2100) & 0x80) != 0 ) && cpu.V_Counter == ScreenHeight + PPU.FIRST_VISIBLE_LINE )
				{
					OAMAddr = SavedOAMAddr;
					int tmp = 0;
					
					if( OAMPriorityRotation )
					{
						tmp = (OAMAddr & 0xFE) >>> 1;
					}
					if( ( ( OAMFlip & 1 ) != 0 ) || FirstSprite != tmp)
					{
						FirstSprite = tmp;
						OBJChanged = true;
					}
					OAMFlip = 0;
				}
				break;

			case 0x2101:
				// Sprite (OBJ) tile address
				if (Byte != memory.FillRAM.get8Bit(0x2101) )
				{
					FLUSH_REDRAW();
					OBJNameBase	 = (Byte & 3) << 14;
					OBJNameSelect = ((Byte >>> 3) & 3) << 13;
					OBJSizeSelect = (Byte >>> 5) & 7;
					OBJChanged = true;
				}
				break;

			case 0x2102:
				// Sprite write address (low)
				OAMAddr = ( ( memory.FillRAM.get16Bit(0x2103) & 1 ) << 8 ) | Byte;
				OAMFlip = 2;
				OAMReadFlip = 0;
				SavedOAMAddr = OAMAddr;
				
				if ( OAMPriorityRotation && FirstSprite != (OAMAddr >>> 1) )
				{
					FirstSprite = (OAMAddr&0xFE) >>> 1;
					OBJChanged = true;
				}
				
				break;

			case 0x2103:
				// Sprite register write address (high), sprite priority rotation
				// bit.
				OAMAddr = ( ( Byte & 1 ) << 8 ) | memory.FillRAM.get8Bit(0x2102);

				OAMPriorityRotation = (Byte & 0x80 ) != 0;
				
				if (OAMPriorityRotation )
				{
					if (FirstSprite != (OAMAddr >>> 1) )
					{
						FirstSprite = (OAMAddr & 0xFE) >>> 1;
						OBJChanged = true;
					}
				} else {
					if (FirstSprite != 0)
					{
						FirstSprite = 0;
						OBJChanged = true;
					}
				}
				OAMFlip = 0;
				OAMReadFlip = 0;
				SavedOAMAddr = OAMAddr;
				break;

			case 0x2104:
				// Sprite register write
				REGISTER_2104(Byte);
				break;

			case 0x2105:

				// Screen mode (0 - 7), background tile sizes and background 3
				// priority
				if (Byte != memory.FillRAM.get8Bit(0x2105) )
				{
					if (BGMode != (Byte & 7))
					{
						GameCanvas.ResetTileLookups();
					}
					
					FLUSH_REDRAW ();
					BG[0].TileSizeMode = (Byte >>> 4) & 1;
					BG[1].TileSizeMode = (Byte >>> 5) & 1;
					BG[2].TileSizeMode = (Byte >>> 6) & 1;
					BG[3].TileSizeMode = (Byte >>> 7) & 1;
					BGMode = Byte & 7;
					// BJ: BG3Priority only takes effect if BGMode==1 and the bit is set
					BG3Priority	= ( Byte & 0x0f ) == 0x09;

					if( BGMode == 5 || BGMode == 6 )
					{
						Interlace = ( memory.FillRAM.getByte(0x2133) & 1) > 0;
					}
					else
					{
						Interlace = false;
					}
					
					if ( SnesSystem.DEBUG_PPU_MODES )
					{
						String BGTileSizes[] = {"8x8", "16x16" };
						System.out.format("BGMode = %d\n", BGMode );
						System.out.format("BG1 Size = %s, ", BGTileSizes[BG[0].TileSizeMode] );
						System.out.format("BG2 Size = %s, ", BGTileSizes[BG[1].TileSizeMode] );
						System.out.format("BG3 Size = %s, ", BGTileSizes[BG[2].TileSizeMode] );
						System.out.format("BG4 Size = %s\n", BGTileSizes[BG[3].TileSizeMode] );
					}
				}
				break;

			case 0x2106:
				if (Byte != memory.FillRAM.get8Bit(0x2106) )
				{
					// Mosaic pixel size and enable
					FLUSH_REDRAW();
					
					MosaicStart = cpu.V_Counter;
					
					if( MosaicStart > ScreenHeight)
					{
						MosaicStart = 0;
					}

					Mosaic = (Byte >>> 4) + 1;
					BGMosaic[0] = (Byte & 1) != 0;
					BGMosaic[1] = (Byte & 2) != 0;
					BGMosaic[2] = (Byte & 4) != 0;
					BGMosaic[3] = (Byte & 8) != 0;
				}
				break;
			case 0x2107:		// [BG0SC]
				if (Byte != memory.FillRAM.get8Bit(0x2107) )
				{
					FLUSH_REDRAW ();
					BG[0].TileMapSize = Byte & 3;
					BG[0].TileMapAddress = (Byte & 0x7c) << 9;
					
					if ( SnesSystem.DEBUG_PPU_MODES )
						System.out.format("BG1 Size = %s, Map Address = %d\n", BGTileDimensions[BG[0].TileMapSize], BG[0].TileMapAddress );

				}
				break;

			case 0x2108:		// [BG1SC]
				if (Byte != memory.FillRAM.get8Bit(0x2108) )
				{
					FLUSH_REDRAW ();
					BG[1].TileMapSize = Byte & 3;
					BG[1].TileMapAddress = (Byte & 0x7c) << 9;
					
					if ( SnesSystem.DEBUG_PPU_MODES )
						System.out.format("BG2 Size = %s, Map Address = %d\n", BGTileDimensions[BG[1].TileMapSize], BG[1].TileMapAddress );
				}
				break;

			case 0x2109:		// [BG2SC]
				if (Byte != memory.FillRAM.get8Bit(0x2109) )
				{
					FLUSH_REDRAW ();
					BG[2].TileMapSize = Byte & 3;
					BG[2].TileMapAddress = (Byte & 0x7c) << 9;
					
					if ( SnesSystem.DEBUG_PPU_MODES )
						System.out.format("BG3 Size = %s, Map Address = %d\n", BGTileDimensions[BG[2].TileMapSize], BG[2].TileMapAddress );
				}
				break;

			case 0x210A:		// [BG3SC]
				if (Byte != memory.FillRAM.get8Bit(0x210a) )
				{
					FLUSH_REDRAW ();
					BG[3].TileMapSize = Byte & 3;
					BG[3].TileMapAddress = (Byte & 0x7c) << 9;
					
					if ( SnesSystem.DEBUG_PPU_MODES)
						System.out.format("BG4 Size = %s, Map Address = %d\n", BGTileDimensions[BG[3].TileMapSize], BG[3].TileMapAddress );
				}
				break;

			case 0x210B:		// [BG01NBA]
				if (Byte != memory.FillRAM.get8Bit(0x210b) )
				{
					FLUSH_REDRAW ();
					BG[0].TileDataBase	= (Byte & 7) << 13;
					BG[1].TileDataBase	= ((Byte >>> 4) & 7) << 13;
					
					if ( SnesSystem.DEBUG_PPU_MODES )
					{
						System.out.format("BG1 Tile Data = %s\n", BG[0].TileDataBase );
						System.out.format("BG2 Tile Data = %s\n", BG[1].TileDataBase );
					}
				}
				break;

			case 0x210C:		// [BG23NBA]
				if (Byte != memory.FillRAM.get8Bit(0x210c) )
				{
					FLUSH_REDRAW ();
					BG[2].TileDataBase	= (Byte & 7) << 13;
					BG[3].TileDataBase	= ((Byte >>> 4) & 7) << 13;
					
					if ( SnesSystem.DEBUG_PPU_MODES )
					{
						System.out.format("BG3 Tile Data = %s\n", BG[2].TileDataBase );
						System.out.format("BG4 Tile Data = %s\n", BG[3].TileDataBase );
					}
				}
				break;


			case 0x210D:
				// Yes, the two formulas are supposed to be different.
				BG[0].HOffset = (short) (( Byte << 8 ) | ( BGnxOFSbyte & ~7 ) | ( ( BG[0].HOffset >>> 8 ) & 7));
				M7HOFS = ( Byte << 8 ) | M7byte;
				BGnxOFSbyte = Byte;
				M7byte = Byte;
				//RegisterBGHorizontalChange(0);
				
				if ( SnesSystem.DEBUG_PPU_MODES && memory.FillRAM.get8Bit(Address) != Byte )
					System.out.format("BG1 HOffset = %d, Line = %d\n", BG[0].HOffset, CurrentLine);
					
				break;

			case 0x210E:
				// Yes, the two formulas are supposed to be different.
				BG[0].VOffset = (short) (( Byte << 8 ) | BGnxOFSbyte);
				M7VOFS = ( Byte << 8 ) | M7byte;
				BGnxOFSbyte = Byte;
				M7byte = Byte;
				//RegisterBGVerticalChange(0);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG1 VOffset = %d, Line = %d\n", BG[0].VOffset, CurrentLine);
				break;

			case 0x210F:
				BG[1].HOffset = (short) (( Byte << 8 ) | ( BGnxOFSbyte & ~7 ) | ( ( BG[1].HOffset >>> 8 ) & 7));
				BGnxOFSbyte = Byte;
				//RegisterBGHorizontalChange(1);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG2 HOffset = %d, Line = %d\n", BG[1].HOffset, CurrentLine);
				break;

			case 0x2110:
				BG[1].VOffset = (short) (( Byte << 8 ) | BGnxOFSbyte);
				BGnxOFSbyte = Byte;
				//RegisterBGVerticalChange(1);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG2 VOffset = %d, Line = %d\n", BG[1].VOffset, CurrentLine);
				break;

			case 0x2111:
				BG[2].HOffset = (short) (( Byte << 8 ) | ( BGnxOFSbyte & ~7 ) | ( ( BG[2].HOffset >>> 8 ) & 7));
				BGnxOFSbyte = Byte;
				//RegisterBGHorizontalChange(2);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG3 HOffset = %d, Line = %d\n", BG[2].HOffset, CurrentLine);
				break;

			case 0x2112:
				BG[2].VOffset = (short) (( Byte << 8 ) | BGnxOFSbyte);
				BGnxOFSbyte = Byte;
				//RegisterBGVerticalChange(2);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG3 VOffset = %d, Line = %d\n", BG[2].VOffset, CurrentLine);
				break;

			case 0x2113:
				BG[3].HOffset = (short) (( Byte << 8 ) | ( BGnxOFSbyte & ~7 ) | ( ( BG[3].HOffset >>> 8 ) & 7));
				BGnxOFSbyte = Byte;
				//RegisterBGHorizontalChange(3);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG4 HOffset = %d, Line = %d\n", BG[3].HOffset, CurrentLine);
				break;

			case 0x2114:
				BG[3].VOffset = (short) (( Byte << 8 ) | BGnxOFSbyte);
				BGnxOFSbyte = Byte;
				//RegisterBGVerticalChange(3);
				
				if ( SnesSystem.DEBUG_PPU_MODES )
					System.out.format("BG4 VOffset = %d, Line = %d\n", BG[3].VOffset, CurrentLine);
				break;

			case 0x2115:
				// VRAM byte/word access flag and increment
				VMA_High = (Byte & 0x80) == 0 ? false : true;
				switch (Byte & 3)
				{
				case 0:
					VMA_Increment = 1;
					break;
				case 1:
					VMA_Increment = 32;
					break;
				case 2:
					VMA_Increment = 128;
					break;
				case 3:
					VMA_Increment = 128;
					break;
				}

				if ( ( Byte & 0x0c ) != 0 )
				{
					final int IncCount[] = { 0, 32, 64, 128 };
					final int Shift[] = { 0, 5, 6, 7 };

					// VMA.Increment = 1;
					int i = (Byte & 0x0c) >>> 2;
					VMA_FullGraphicCount = IncCount [i];
					VMA_Mask1 = IncCount [i] * 8 - 1;
					VMA_Shift = Shift [i];
				}
				else
				{
					VMA_FullGraphicCount = 0;
				}
				break;

			case 0x2116:
				// VRAM read/write address (low)
				VMA_Address &= 0xFF00;
				VMA_Address |= Byte;

				if (VMA_FullGraphicCount > 0)
				{
					int addr = VMA_Address;
					int rem = addr & VMA_Mask1;
					int address = (addr & ~ VMA_Mask1) +
						(rem >>> VMA_Shift) +
						((rem & (VMA_FullGraphicCount - 1)) << 3);
					VRAMReadBuffer = memory.VRAM.get16Bit( ( address << 1 ) & 0xFFFF);
				}
				else
				{
					VRAMReadBuffer = memory.VRAM.get16Bit( ( VMA_Address << 1 ) & 0xFFFF);
				}

				break;

			case 0x2117:
				// VRAM read/write address (high)
				VMA_Address &= 0x00FF;
				VMA_Address |= Byte << 8;

				if (VMA_FullGraphicCount > 0)
				{
					int addr = VMA_Address;
					int rem = addr & VMA_Mask1;
					int address = (addr & ~VMA_Mask1) +
						(rem >>> VMA_Shift) +
						((rem & (VMA_FullGraphicCount - 1)) << 3);
					VRAMReadBuffer = memory.VRAM.get16Bit( ( address << 1 ) & 0xFFFF);
				}
				else
				{
					VRAMReadBuffer = memory.VRAM.get16Bit( ( VMA_Address << 1 ) & 0xFFFF);
				}

				break;

			case 0x2118:
				// VRAM write data (low)
				FirstVRAMRead = true;
				REGISTER_2118(Byte);
				break;

			case 0x2119:
				// VRAM write data (high)
				FirstVRAMRead = true;
				REGISTER_2119(Byte);
				break;

			case 0x211a:
				// Mode 7 outside rotation area display mode and flipping
				if (Byte != memory.FillRAM.get8Bit(0x211a) )
				{
					FLUSH_REDRAW ();
					Mode7Repeat = Byte >>> 6;
					if (Mode7Repeat == 1)
						Mode7Repeat = 0;
					Mode7VFlip = ( (Byte & 2) >>> 1 ) > 0;
					Mode7HFlip = ( Byte & 1 ) > 0;
				}
				break;
			case 0x211b:
				// Mode 7 matrix A (low & high)
				MatrixA = M7byte | (Byte << 8);
				Need16x8Mulitply = true;
				M7byte = Byte;
				break;
			case 0x211c:
				// Mode 7 matrix B (low & high)
				MatrixB = M7byte | (Byte << 8);
				Need16x8Mulitply = true;
				M7byte = Byte;
				break;
			case 0x211d:
				// Mode 7 matrix C (low & high)
				MatrixC = M7byte | (Byte << 8);
				M7byte = Byte;
				break;
			case 0x211e:
				// Mode 7 matrix D (low & high)
				MatrixD = M7byte | (Byte << 8);
				M7byte = Byte;
				break;
			case 0x211f:
				// Mode 7 centre of rotation X (low & high)
				CentreX = M7byte | (Byte << 8);
				M7byte = Byte;
				break;
			case 0x2120:
				// Mode 7 centre of rotation Y (low & high)
				CentreY = M7byte | (Byte << 8);
				M7byte = Byte;
				break;

			case 0x2121:
				// CG-RAM address
				CGFLIP = false;
				CGFLIPRead = 0;
				CGADD = Byte;
				break;

			case 0x2122:
				REGISTER_2122(Byte);
				break;

			case 0x2123:
				// Window 1 and 2 enable for backgrounds 1 and 2
				if (Byte != memory.FillRAM.get8Bit(0x2123) )
				{
					FLUSH_REDRAW ();
					// NAC: For some reason there was !! operators here
					ClipWindow1Enable[0] = (Byte & 0x02) > 0;
					ClipWindow1Enable[1] = (Byte & 0x20) > 0;
					ClipWindow2Enable[0] = (Byte & 0x08) > 0;
					ClipWindow2Enable[1] = (Byte & 0x80) > 0;
					ClipWindow1Inside[0] = (Byte & 0x01) != 0;
					ClipWindow1Inside[1] = (Byte & 0x10) != 0;
					ClipWindow2Inside[0] = (Byte & 0x04) != 0;
					ClipWindow2Inside[1] = (Byte & 0x40) != 0;
					RecomputeClipWindows = true;

				}
				break;
			case 0x2124:
				// Window 1 and 2 enable for backgrounds 3 and 4
				if (Byte != memory.FillRAM.get8Bit(0x2124) )
				{
					FLUSH_REDRAW ();
					ClipWindow1Enable[2] = (Byte & 0x02) != 0;
					ClipWindow1Enable[3] = (Byte & 0x20) != 0;
					ClipWindow2Enable[2] = (Byte & 0x08) != 0;
					ClipWindow2Enable[3] = (Byte & 0x80) != 0;
					ClipWindow1Inside[2] = (Byte & 0x01) == 0;
					ClipWindow1Inside[3] = (Byte & 0x10) == 0;
					ClipWindow2Inside[2] = (Byte & 0x04) == 0;
					ClipWindow2Inside[3] = (Byte & 0x40) == 0;
					RecomputeClipWindows = true;

				}
				break;
			case 0x2125:
				// Window 1 and 2 enable for objects and colour window
				if (Byte != memory.FillRAM.get8Bit(0x2125) )
				{
					FLUSH_REDRAW ();
					ClipWindow1Enable[4] = (Byte & 0x02) != 0;
					ClipWindow1Enable[5] = (Byte & 0x20) != 0;
					ClipWindow2Enable[4] = (Byte & 0x08) != 0;
					ClipWindow2Enable[5] = (Byte & 0x80) != 0;
					ClipWindow1Inside[4] = (Byte & 0x01) == 0;
					ClipWindow1Inside[5] = (Byte & 0x10) == 0;
					ClipWindow2Inside[4] = (Byte & 0x04) == 0;
					ClipWindow2Inside[5] = (Byte & 0x40) == 0;
					RecomputeClipWindows = true;
				}
				break;
			case 0x2126:
				// Window 1 left position
				if (Byte != memory.FillRAM.get8Bit(0x2126) )
				{
					FLUSH_REDRAW ();
					Window1Left = Byte;
					RecomputeClipWindows = true;
				}
				break;
			case 0x2127:
				// Window 1 right position
				if (Byte != memory.FillRAM.get8Bit(0x2127) )
				{
					FLUSH_REDRAW ();
					Window1Right = Byte;
					RecomputeClipWindows = true;
				}
				break;
			case 0x2128:
				// Window 2 left position
				if (Byte != memory.FillRAM.get8Bit(0x2128) )
				{
					FLUSH_REDRAW ();
					Window2Left = Byte;
					RecomputeClipWindows = true;
				}
				break;
			case 0x2129:
				// Window 2 right position
				if (Byte != memory.FillRAM.get8Bit(0x2129) )
				{
					FLUSH_REDRAW ();
					Window2Right = Byte;
					RecomputeClipWindows = true;
				}
				break;
			case 0x212a:
				// Windows 1 & 2 overlap logic for backgrounds 1 - 4
				if (Byte != memory.FillRAM.get8Bit(0x212a) )
				{
					FLUSH_REDRAW ();
					ClipWindowOverlapLogic[0] = (Byte & 0x03);
					ClipWindowOverlapLogic[1] = (Byte & 0x0c) >>> 2;
					ClipWindowOverlapLogic[2] = (Byte & 0x30) >>> 4;
					ClipWindowOverlapLogic[3] = (Byte & 0xc0) >>> 6;
					RecomputeClipWindows = true;
				}
				break;
			case 0x212b:
				// Windows 1 & 2 overlap logic for objects and colour window
				if (Byte != memory.FillRAM.get8Bit(0x212b) )
				{
					FLUSH_REDRAW ();
					ClipWindowOverlapLogic[4] = Byte & 0x03;
					ClipWindowOverlapLogic[5] = (Byte & 0x0c) >>> 2;
					RecomputeClipWindows = true;
				}
				break;
			case 0x212c:
				// Main screen designation (backgrounds 1 - 4 and objects)
				if (Byte != memory.FillRAM.get8Bit(0x212c) )
				{
					FLUSH_REDRAW ();
					RecomputeClipWindows = true;
					memory.FillRAM.put8Bit(Address, Byte);
					return;
				}
				break;
			case 0x212d:
				// Sub-screen designation (backgrounds 1 - 4 and objects)
				if (Byte != memory.FillRAM.get8Bit(0x212d) )
				{
					FLUSH_REDRAW ();

					RecomputeClipWindows = true;
					memory.FillRAM.put8Bit(Address, Byte);
					return;
				}
				break;
			case 0x212e:
				// Window mask designation for main screen
				if (Byte != memory.FillRAM.get8Bit(0x212e) )
				{
					FLUSH_REDRAW ();
					RecomputeClipWindows = true;
				}
				break;
			case 0x212f:
				// Window mask designation for sub-screen
				if (Byte != memory.FillRAM.get8Bit(0x212f) )
				{
					FLUSH_REDRAW ();
					RecomputeClipWindows = true;
				}
				break;
			case 0x2130:
				// Fixed colour addition or screen addition
				if (Byte != memory.FillRAM.get8Bit(0x2130) )
				{
					FLUSH_REDRAW ();
					RecomputeClipWindows = true;
				}
				break;
			case 0x2131:
				// Colour addition or subtraction select
				if (Byte != memory.FillRAM.get8Bit(0x2131) )
				{
					FLUSH_REDRAW ();

					// Backgrounds 1 - 4, objects and backdrop colour add/sub enable
					memory.FillRAM.put8Bit(0x2131, Byte);
					
					BackgroundChanged();
				}
				break;
			case 0x2132:
				if (Byte != memory.FillRAM.get8Bit(0x2132) )
				{
					FLUSH_REDRAW ();
					
					// Colour data for fixed colour addition/subtraction
					if ( ( Byte & 0x80 ) != 0 )
						FixedColourBlue = Byte & 0x1f;
					if ( ( Byte & 0x40 ) != 0 )
						FixedColourGreen = Byte & 0x1f;
					if ( ( Byte & 0x20 ) != 0 )
						FixedColourRed = Byte & 0x1f;
					
					BackgroundChanged();
				}
				break;
			case 0x2133:
				// Screen settings
				if ( Byte != memory.FillRAM.get8Bit(0x2133) )
				{

					if( ( ( memory.FillRAM.get8Bit(0x2133) ^ Byte ) & 8 ) == 8 )
					{
						FLUSH_REDRAW ();
						PseudoHires = ( Byte & 8 ) == 8;
					}
					
					if ( ( Byte & 0x04 ) != 0)
					{
						ScreenHeight = SnesSystem.SNES_HEIGHT_EXTENDED;
						
						if( DoubleHeightPixels )
						{
							RenderedScreenHeight = ScreenHeight << 1;
						}
						else
						{
							RenderedScreenHeight = ScreenHeight;
						}

					}
					else
					{
						ScreenHeight = SnesSystem.SNES_HEIGHT;
					}


					if( ( ( memory.FillRAM.get8Bit(0x2133) ^ Byte ) & 3 ) > 0 )
					{
						FLUSH_REDRAW();
						
						if( ( ( memory.FillRAM.get8Bit(0x2133) ^ Byte ) & 2 ) == 2 )
						{
							OBJChanged = true;
						}
						
						if( BGMode == 5 || BGMode == 6 )
						{
							Interlace = ( Byte & 1 ) == 1;
						}
						
						InterlaceOBJ = ( Byte & 2 ) == 2;
					}

				}
				break;
			case 0x2134:
			case 0x2135:
			case 0x2136:
				// Matrix 16bit x 8bit multiply result (read-only)
				return;

			case 0x2137:
				// Software latch for horizontal and vertical timers (read-only)
				return;
			case 0x2138:
				// OAM read data (read-only)
				return;
			case 0x2139:
			case 0x213a:
				// VRAM read data (read-only)
				return;
			case 0x213b:
				// CG-RAM read data (read-only)
				return;
			case 0x213c:
			case 0x213d:
				// Horizontal and vertical (low/high) read counter (read-only)
				return;
			case 0x213e:
				// PPU status (time over and range over)
				return;
			case 0x213f:
				// NTSC/PAL select and field (read-only)
				return;
			case 0x2140: case 0x2141: case 0x2142: case 0x2143:
			case 0x2144: case 0x2145: case 0x2146: case 0x2147:
			case 0x2148: case 0x2149: case 0x214a: case 0x214b:
			case 0x214c: case 0x214d: case 0x214e: case 0x214f:
			case 0x2150: case 0x2151: case 0x2152: case 0x2153:
			case 0x2154: case 0x2155: case 0x2156: case 0x2157:
			case 0x2158: case 0x2159: case 0x215a: case 0x215b:
			case 0x215c: case 0x215d: case 0x215e: case 0x215f:
			case 0x2160: case 0x2161: case 0x2162: case 0x2163:
			case 0x2164: case 0x2165: case 0x2166: case 0x2167:
			case 0x2168: case 0x2169: case 0x216a: case 0x216b:
			case 0x216c: case 0x216d: case 0x216e: case 0x216f:
			case 0x2170: case 0x2171: case 0x2172: case 0x2173:
			case 0x2174: case 0x2175: case 0x2176: case 0x2177:
			case 0x2178: case 0x2179: case 0x217a: case 0x217b:
			case 0x217c: case 0x217d: case 0x217e: case 0x217f:

				apu.APUExecuting = settings.APUEnabled;
				apu.WaitCounter++;

				apu.APUExecute();
				
				// NAC: Possible optimization. Remove this following line
				// because it is called again at the end of this routine
				memory.FillRAM.put8Bit(Address, Byte);
				
				// NAC: This is how the CPU talks to the spc700
				//System.out.println( String.format("spc700 SetByte: R%d to %02X", (Address & 3), Byte ));
				apu.RAM.put8Bit( (Address & 3) + 0xf4, Byte);
				
				break;
			case 0x2180:
				if( ! cpu.InWRAMDMAorHDMA )
				{
					REGISTER_2180(Byte);
				}
				break;
			case 0x2181:
				if( ! cpu.InWRAMDMAorHDMA)
				{
					WRAM &= 0x1FF00;
					WRAM |= Byte;
				}
				break;
			case 0x2182:
				if( ! cpu.InWRAMDMAorHDMA)
				{
					WRAM &= 0x100FF;
					WRAM |= Byte << 8;
				}
				break;
			case 0x2183:
				if( ! cpu.InWRAMDMAorHDMA)
				{
					WRAM &= 0x0FFFF;
					WRAM |= Byte << 16;
					WRAM &= 0x1FFFF;
				}
				break;

			case 0x2188:
			case 0x2189:
			case 0x218a:
			case 0x218b:
			case 0x218c:
			case 0x218d:
			case 0x218e:
			case 0x218f:
			case 0x2190:
			case 0x2191:
			case 0x2192:
			case 0x2193:
			case 0x2194:
			case 0x2195:
			case 0x2196:
			case 0x2197:
			case 0x2198:
			case 0x2199:
			case 0x219a:
			case 0x219b:
			case 0x219c:
			case 0x219d:
			case 0x219e:
			case 0x219f:
				// NAC: Disabled
				/*
				if (Settings.BS)
					SetBSXPPU(Byte, Address);
				*/
				break;
	 		}
		}
		else
		{
			if (settings.SA1)
			{
				if (Address >= 0x2200 && Address < 0x23ff)
				{
					sa1.SetCPU(Byte, Address);
				}
				else
				{
					memory.FillRAM.put8Bit(Address, Byte);
				}

				return;
			}
			/*
			else
			{
				// Dai Kaijyu Monogatari II
				if (Address == 0x2801 && settings.SRTC)
				{
					globals.rtc.SetSRTC(Byte, Address);
				}
				else if (Address < 0x3000 || Address >= 0x3000 + 768)
				{

				}
				else
				{
					//NAC: SuperFX disabled
					
					if ( ! settings.SuperFX)
					{
						return;
					}

					switch (Address)
					{
					case 0x3030:
							
						if ( ( ( memory.FillRAM.get8Bit(0x3030) ^ Byte ) & SuperFX.FLG_G ) > 0 )
						{
							memory.FillRAM.put8Bit(Address, Byte);
							
							// Go flag has been changed
							if ( ( Byte & SuperFX.FLG_G ) > 0)
							{
								if ( ! globals.superfx.oneLineDone)
								{
									SuperFXExec();
									globals.superfx.oneLineDone = true;
								}
							}
							else
							{
								superfx.FxFlushCache();
							}
						}
						else
						{
							memory.FillRAM.put8Bit(Address, Byte);
						}
						break;

					case 0x3031:
						memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x3033:
						memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x3034:
						memory.FillRAM.put8Bit(Address, Byte & 0x7f);
						break;
					case 0x3036:
						memory.FillRAM.put8Bit(Address, Byte & 0x7f);
						break;
					case 0x3037:
						memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x3038:
						memory.FillRAM.put8Bit(Address, Byte);
						superfx.fx_dirtySCBR();
						break;
					case 0x3039:
						memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x303a:
						memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x303b:
						break;
					case 0x303c:
						memory.FillRAM.put8Bit(Address, Byte);
						superfx.fx_updateRamBank(Byte);
						break;
					case 0x303f:
							memory.FillRAM.put8Bit(Address, Byte);
						break;
					case 0x301f:
						memory.FillRAM.put8Bit(Address, Byte);
						memory.FillRAM.put8Bit(0x3000 + SuperFX.GSU_SFR, memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_SFR) | SuperFX.FLG_G);
						if ( ! globals.superfx.oneLineDone)
						{								
							SuperFXExec();
							globals.superfx.oneLineDone = true;
						}
						return;

					default:
						memory.FillRAM.put8Bit(Address, Byte);

						if (Address >= 0x3100)
						{
							superfx.FxCacheWriteAccess (Address);
						}
						break;
					}
					return;
					
				}
				
			}
			*/
		}
		memory.FillRAM.put8Bit(Address, Byte);
	}
	
	int GetPPU( int Address )
	{
	 	int Byte = globals.OpenBus;

		if(Address < 0x2100) //not a real PPU reg
			return globals.OpenBus; //treat as unmapped memory returning last Byte on the bus

		// Take care of DMA wrapping
		if( cpu.InDMAorHDMA && Address > 0x21ff)
			Address = 0x2100 + ( Address & 0xff );

		if( Address <= 0x219F )
		{
		 	switch (Address)
			{
			case 0x2100:
			case 0x2101:
			case 0x2102:
			case 0x2103:
				return globals.OpenBus;
	
			case 0x2104:
			case 0x2105:
			case 0x2106:
				return OpenBus1;
			case 0x2107:
				return globals.OpenBus;
			case 0x2108:
			case 0x2109:
			case 0x210a:
				return OpenBus1;
			case 0x210b:
			case 0x210c:
			case 0x210d:
			case 0x210e:
			case 0x210f:
			case 0x2110:
			case 0x2111:
			case 0x2112:
			case 0x2113:
				//missing.bg_offset_read = 1;
				return globals.OpenBus;
	
			case 0x2114:
			case 0x2115:
			case 0x2116:
				return OpenBus1;
	
			case 0x2117:
				return globals.OpenBus;
	
			case 0x2118:
			case 0x2119:
			case 0x211a:
				return OpenBus1;
	
			case 0x211b:
			case 0x211c:
			case 0x211d:
			case 0x211e:
			case 0x211f:
			case 0x2120:
				return globals.OpenBus;
	
			case 0x2121:
			case 0x2122:
			case 0x2123:
				return globals.OpenBus;
	
			case 0x2124:
			case 0x2125:
			case 0x2126:
				return OpenBus1;
	
			case 0x2127:
				return globals.OpenBus;
	
			case 0x2128:
			case 0x2129:
			case 0x212a:
				return OpenBus1;
	
			case 0x212b:
			case 0x212c:
			case 0x212d:
			case 0x212e:
			case 0x212f:
			case 0x2130:
			case 0x2131:
			case 0x2132:
			case 0x2133:
				return globals.OpenBus;
	
			case 0x2134:
			case 0x2135:
			case 0x2136:
				// 16bit x 8bit multiply read result.
				if (Need16x8Mulitply)
				{
					int r = (int) MatrixA * (int) (MatrixB >>> 8);
		
					memory.FillRAM.put8Bit(0x2134, r);
					memory.FillRAM.put8Bit(0x2135, r >>> 8);
					memory.FillRAM.put8Bit(0x2136, r >>> 16);
					Need16x8Mulitply = false;
				}
	
				return (OpenBus1 = memory.FillRAM.get8Bit(Address) );
			case 0x2137:
				LatchCounters(false);
				return globals.OpenBus;
	
			case 0x2138:
				// Read OAM (sprite) control data
				if( ( OAMAddr & 0x100 ) > 0 )
				{
					if ( (OAMFlip & 1) == 0)
					{
						Byte = OAMData.get8Bit( (OAMAddr & 0x10f) << 1);
					}
					else
					{
						Byte = OAMData.get8Bit( ( (OAMAddr&0x10f) << 1) + 1 );
						OAMAddr = ( OAMAddr + 1) & 0x1ff;
						if ( OAMPriorityRotation && FirstSprite != (OAMAddr >>> 1) )
						{
							FirstSprite = (OAMAddr & 0xFE) >>> 1;
							OBJChanged = true;
						}
					}
				}
				else
				{
					if ( (OAMFlip & 1) == 0)
					{
						Byte = OAMData.get8Bit(OAMAddr << 1);
					}
					else
					{
						Byte = OAMData.get8Bit( (OAMAddr << 1) + 1);
						++OAMAddr;
						if ( OAMPriorityRotation && FirstSprite != (OAMAddr >>> 1) )
						{
							FirstSprite = (OAMAddr & 0xFE) >>> 1;
							OBJChanged = true;
						}
					}
				}
				OAMFlip ^= 1;
				return (OpenBus1 = Byte);
	
			case 0x2139:
				// Read vram low Byte
	
				Byte = VRAMReadBuffer & 0xff;
				if ( ! VMA_High )
				{
					if (VMA_FullGraphicCount > 0)
					{
						int addr = VMA_Address;
						int rem = addr & VMA_Mask1;
						int address = (addr & ~VMA_Mask1) +
							(rem >>> VMA_Shift) +
							((rem & (VMA_FullGraphicCount - 1)) << 3);
						VRAMReadBuffer = memory.VRAM.get16Bit((address << 1) & 0xFFFF);
					} 
					else
					{
						VRAMReadBuffer = memory.VRAM.get16Bit((VMA_Address << 1) & 0xffff);
					}
					VMA_Address += VMA_Increment;
				}
	
				OpenBus1 = Byte;
				break;
			case 0x213A:
				// Read vram high Byte
				Byte = (VRAMReadBuffer >>> 8) & 0xff;
				
				if (VMA_High)
				{
					if (VMA_FullGraphicCount > 0)
					{
						int addr = VMA_Address;
						int rem = addr & VMA_Mask1;
						int address = (addr & ~VMA_Mask1) +
							(rem >>> VMA_Shift) +
							((rem & (VMA_FullGraphicCount - 1)) << 3);
						VRAMReadBuffer = memory.VRAM.get16Bit( (address << 1) & 0xFFFF);
					} else
						VRAMReadBuffer = memory.VRAM.get16Bit( (VMA_Address << 1) & 0xffff);
					VMA_Address += VMA_Increment;
				}
	
				OpenBus1 = Byte;
				break;
	
			case 0x213B:
				// Read palette data
				if (CGFLIPRead != 0)
				{
					CGADD = ( CGADD + 1 ) & 0xFF;
					Byte = CGDATA[CGADD] >>> 8;
				}
				else
				{
					Byte = CGDATA[CGADD] & 0xff;
				}
	
				CGFLIPRead ^= 1;
				return (OpenBus2 = Byte);
	
			case 0x213C:
				// Horizontal counter value 0-339
				//TryGunLatch(false);
				if (HBeamFlip > 0)
				{
					Byte = (OpenBus2 & 0xfe) | ((HBeamPosLatched >>> 8) & 0x01);
				}
				else
				{
					Byte = HBeamPosLatched & 0xFF;
				}
				
				OpenBus2 = Byte;
				HBeamFlip ^= 1;
				break;
	
			case 0x213D:
				// Vertical counter value 0-262	
				//TryGunLatch(false);
				
				if (VBeamFlip > 0)
				{
					Byte = (OpenBus2 & 0xfe) | ((VBeamPosLatched >>> 8) & 0x01);
				}
				else
				{
					Byte = VBeamPosLatched & 0xFF;
				}
				
				OpenBus2 = Byte;
				VBeamFlip ^= 1;
				break;
	
			case 0x213E:
				// PPU time and range over flags
				FLUSH_REDRAW ();
	
				//so far, 5c77 version is always 1.
				return (OpenBus1 = (SnesSystem._5C77 | RangeTimeOver));
	
			case 0x213F:
				// NTSC/PAL and which field flags
				//TryGunLatch(false);
				VBeamFlip = HBeamFlip = 0;
				//neviksti found a 2 and a 3 here. SNEeSe uses a 3.
				Byte = ((settings.PAL ? 0x10 : 0) | (memory.FillRAM.getByte(0x213f) & 0xc0) | 
						SnesSystem._5C78) | (OpenBus2 & 0x20);
				memory.FillRAM.put8Bit(0x213f, memory.FillRAM.getByte(0x213f) & ~0x40);
				return Byte;
	
			case 0x2140: case 0x2141: case 0x2142: case 0x2143:
			case 0x2144: case 0x2145: case 0x2146: case 0x2147:
			case 0x2148: case 0x2149: case 0x214a: case 0x214b:
			case 0x214c: case 0x214d: case 0x214e: case 0x214f:
			case 0x2150: case 0x2151: case 0x2152: case 0x2153:
			case 0x2154: case 0x2155: case 0x2156: case 0x2157:
			case 0x2158: case 0x2159: case 0x215a: case 0x215b:
			case 0x215c: case 0x215d: case 0x215e: case 0x215f:
			case 0x2160: case 0x2161: case 0x2162: case 0x2163:
			case 0x2164: case 0x2165: case 0x2166: case 0x2167:
			case 0x2168: case 0x2169: case 0x216a: case 0x216b:
			case 0x216c: case 0x216d: case 0x216e: case 0x216f:
			case 0x2170: case 0x2171: case 0x2172: case 0x2173:
			case 0x2174: case 0x2175: case 0x2176: case 0x2177:
			case 0x2178: case 0x2179: case 0x217a: case 0x217b:
			case 0x217c: case 0x217d: case 0x217e: case 0x217f:
	
				apu.APUExecuting = settings.APUEnabled;
				apu.WaitCounter++;
				apu.APUExecute();
				if (settings.APUEnabled)
				{
					return (apu.OutPorts[Address & 3]);
				}
	
				switch (settings.SoundSkipMethod)
				{
				case 0:
				case 1:
					cpu.BranchSkip = true;
					break;
				case 2:
					break;
				case 3:
					cpu.BranchSkip = true;
					break;
				}
				
				random++;
				
				if ( (Address & 3) < 2)
				{
					if ( ( random & 2 ) > 0 )
					{
						if ( ( random & 4 ) > 0 )
						{
							return ((Address & 3) == 1 ? 0xaa : 0xbb);
						}
						else
						{
							return ((random >>> 3) & 0xff);
						}
					}
				}
				else
				{
					if ( ( random & 2 ) > 0 )
					{
						return ((random >>> 3) & 0xff);
					}
				}
				
				return memory.FillRAM.get8Bit(Address);
	
			case 0x2180:
				// Read WRAM
				if( ! cpu.InWRAMDMAorHDMA )
				{
						Byte = memory.RAM.get8Bit(WRAM++);
						WRAM &= 0x1FFFF;
				} else {
						Byte=globals.OpenBus;
				}
				break;
			case 0x2181:
			case 0x2182:
			case 0x2183:
			case 0x2184:
			case 0x2185:
			case 0x2186:
			case 0x2187:
					return globals.OpenBus;
			/*
			case 0x2188:
			case 0x2189:
			case 0x218a:
			case 0x218b:
			case 0x218c:
			case 0x218d:
			case 0x218e:
			case 0x218f:
			case 0x2190:
			case 0x2191:
			case 0x2192:
			case 0x2193:
			case 0x2194:
			case 0x2195:
			case 0x2196:
			case 0x2197:
			case 0x2198:
			case 0x2199:
			case 0x219a:
			case 0x219b:
			case 0x219c:
			case 0x219d:
			case 0x219e:
			case 0x219f:
				if (settings.BS)
				{
					//TODO: return GetBSXPPU(Address);
				}
				else
				{
					return globals.OpenBus;
				}
			*/
			default:
				return globals.OpenBus;
			}
		 	
		}
		else
		{
				
			if (settings.SA1)
			{
				return sa1.GetCPU(Address);
			}
	
			if (Address <= 0x2fff || Address >= 0x3000 + 768)
			{
				switch (Address)
				{
				case 0x21c2:
					if(SnesSystem._5C77 == 2 )
						return (0x20);

					return globals.OpenBus;
				case 0x21c3:
					if(SnesSystem._5C77 ==2)
						return (0);

					return globals.OpenBus;
				case 0x2800:
					// For Dai Kaijyu Monogatari II
					if (settings.SRTC)
					{
						return globals.rtc.GetSRTC(Address);
					}
	
				default:
					return globals.OpenBus;
				}
			}

			if ( ! settings.SuperFX)
				return globals.OpenBus;
		}

		return Byte;
	}
	
	final void SetCPU (int Byte, int Address)
	{		
		int d;
		
		if ( SnesSystem.DEBUG_CPU )
			System.out.println( String.format("CPU SetByte: %02X to %04X", Byte, Address ));
		
		if (Address < 0x4200)
		{
			cpu.Cycles += SnesSystem.ONE_CYCLE;
			switch (Address)
			{
			case 0x4016:
				globals.controls.SetJoypadLatch( ( Byte & 1 ) == 1);
				break;
			case 0x4017:
				break;
			default:
				break;
			}
		}
		else
			switch (Address)
			{
			case 0x4200:

				if ( ( Byte & 0x20 ) != 0 )
				{
					VTimerEnabled = true;

				}
				else
				{
					VTimerEnabled = false;
				}

				if ( ( Byte & 0x10 ) != 0 )
				{
					HTimerEnabled = true;

				}
				else
				{
					HTimerEnabled = false;
				}

				UpdateHVTimerPosition();

				// The case that IRQ will trigger in an instruction such as STA $4200
				// FIXME: not true but good enough, I think.
				CheckMissingHTimerRange(cpu.PrevCycles, cpu.Cycles - cpu.PrevCycles);

				if ( ( Byte & 0x30 ) == 0 )
				{
					cpu.ClearIRQSource (PPU.PPU_V_BEAM_IRQ_SOURCE | PPU.PPU_H_BEAM_IRQ_SOURCE);
				}

				if( ( ( Byte & 0x80) != 0 ) && 
					( ( memory.FillRAM.getByte(0x4200) & 0x80) == 0 ) &&
					cpu.V_Counter >= ( ScreenHeight + PPU.FIRST_VISIBLE_LINE ) && 
					( (memory.FillRAM.getByte(0x4210) & 0x80) > 0 )
				)
				{
					// FIXME: triggered at HC=6, checked just before the final CPU cycle,
					// then, when to call Opcode_NMI()?
					cpu.Flags |= SnesSystem.NMI_FLAG;
					timings.NMITriggerPos = cpu.Cycles + 6 + 6;
				}
				break;

			case 0x4201:
				if( ( Byte & 0x80 ) == 0 && (memory.FillRAM.getByte(0x4213) & 0x80) != 0 )
				{
					LatchCounters(true);
				}
				
				memory.FillRAM.put8Bit(0x4201, Byte); 
				memory.FillRAM.put8Bit(0x4213, Byte);
				break;
				
			case 0x4202:
				// Multiplier (for multply)
				break;
			case 0x4203:

				// Multiplicand
				int res = memory.FillRAM.get8Bit(0x4202) * Byte;

				memory.FillRAM.put8Bit(0x4216, res);
				memory.FillRAM.put8Bit(0x4217, (res >>> 8));
				break;

			case 0x4204:
			case 0x4205:
				// Low and high muliplier (for divide)
				break;
			case 0x4206:
				// Divisor
				int a = memory.FillRAM.get8Bit(0x4204) + (memory.FillRAM.get8Bit(0x4205) << 8);
				int div = Byte > 0 ? a / Byte : 0xffff;
				int rem = Byte > 0 ? a % Byte : a;

				memory.FillRAM.put8Bit(0x4214, div);
				memory.FillRAM.put8Bit(0x4215, div >>> 8);
				memory.FillRAM.put8Bit(0x4216, rem);
				memory.FillRAM.put8Bit(0x4217, rem >>> 8);
				break;

			case 0x4207:
				d = IRQHBeamPos;
				IRQHBeamPos = (IRQHBeamPos & 0xFF00) | Byte;

				if (IRQHBeamPos != d)
					UpdateHVTimerPosition();

				break;

			case 0x4208:
				d = IRQHBeamPos;
				IRQHBeamPos = (IRQHBeamPos & 0xFF) | ((Byte & 1) << 8);

				if (IRQHBeamPos != d)
					UpdateHVTimerPosition();

				break;

			case 0x4209:
				d = IRQVBeamPos;
				IRQVBeamPos = (IRQVBeamPos & 0xFF00) | Byte;

				if (IRQVBeamPos != d)
					UpdateHVTimerPosition();

				break;

			case 0x420A:
				d = IRQVBeamPos;
				IRQVBeamPos = (IRQVBeamPos & 0xFF) | ( (Byte & 1) << 8);

				if (IRQVBeamPos != d)
					UpdateHVTimerPosition();

				break;

			case 0x420B:
				if(cpu.InDMAorHDMA) return;

				// XXX: Not quite right...
				if ( Byte  != 0 )
				{
					cpu.Cycles += timings.DMACPUSync;
				}
				
				if ( (Byte & 0x01) != 0)
				{
					dma.DoDMA(0);
				}
				if ( (Byte & 0x02) != 0)
				{
					dma.DoDMA(1);
				}
				if ( (Byte & 0x04) != 0)
				{
					dma.DoDMA(2);
				}
				if ( (Byte & 0x08) != 0)
				{
					dma.DoDMA(3);
				}
				if ( (Byte & 0x10) != 0)
				{
					dma.DoDMA(4);
				}
				if ( (Byte & 0x20) != 0)
				{
					dma.DoDMA(5);
				}
				if ( (Byte & 0x40) != 0)
				{
					dma.DoDMA(6);
				}
				if ( (Byte & 0x80) != 0)
				{
					dma.DoDMA(7);
				}
				break;
			case 0x420C:
				if( cpu.InDMAorHDMA ) return;

				memory.FillRAM.put8Bit(0x420c,  Byte);

				// FIXME
				// Yoshi's Island / Genjyu Ryodan, Mortal Kombat, Tales of Phantasia
				HDMA = Byte & ~HDMAEnded;
				break;

			case 0x420d:
				// Cycle speed 0 - 2.68Mhz, 1 - 3.58Mhz (banks 0x80 +)
				if ((Byte & 1) != (memory.FillRAM.getByte(0x420d) & 1))
				{
					if ( ( Byte & 1 ) != 0 )
					{
						cpu.FastROMSpeed = SnesSystem.ONE_CYCLE;

					}
					else
					{
						cpu.FastROMSpeed = SnesSystem.SLOW_ONE_CYCLE;
					}

					memory.FixROMSpeed();
				}
				break;

			case 0x420e:
			case 0x420f:
				// --->>> Unknown
				break;
			case 0x4210:
				// NMI ocurred flag (reset on read or write)
				memory.FillRAM.put8Bit(0x4210, SnesSystem._5A22);
				return;
			case 0x4211:
				// IRQ ocurred flag (reset on read or write)
				cpu.ClearIRQSource (PPU.PPU_V_BEAM_IRQ_SOURCE | PPU.PPU_H_BEAM_IRQ_SOURCE);
				break;
			case 0x4212:
				// v-blank, h-blank and joypad being scanned flags (read-only)
			case 0x4213:
				// I/O Port (read-only)
			case 0x4214:
			case 0x4215:
				// Quotent of divide (read-only)
			case 0x4216:
			case 0x4217:
				// Multiply product (read-only)
				return;
			case 0x4218:
			case 0x4219:
			case 0x421a:
			case 0x421b:
			case 0x421c:
			case 0x421d:
			case 0x421e:
			case 0x421f:
				// Joypad values (read-only)
				return;

			case 0x4300:
			case 0x4310:
			case 0x4320:
			case 0x4330:
			case 0x4340:
			case 0x4350:
			case 0x4360:
			case 0x4370:
				if(cpu.InDMAorHDMA) return;
				
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].ReverseTransfer = (Byte & 0x80) > 0;
				dma.dmabanks[d].HDMAIndirectAddressing = (Byte & 0x40) > 0;
				dma.dmabanks[d].UnusedBit43x0 = (Byte & 0x20) > 0;
				dma.dmabanks[d].AAddressDecrement = (Byte & 0x10) > 0;
				dma.dmabanks[d].AAddressFixed = (Byte & 0x08) > 0;
				dma.dmabanks[d].TransferMode = (Byte & 7);
				return;

			case 0x4301:
			case 0x4311:
			case 0x4321:
			case 0x4331:
			case 0x4341:
			case 0x4351:
			case 0x4361:
			case 0x4371:
				if(cpu.InDMAorHDMA) return;
				dma.dmabanks[((Address >>> 4) & 0x7)].BAddress = Byte;
				return;

			case 0x4302:
			case 0x4312:
			case 0x4322:
			case 0x4332:
			case 0x4342:
			case 0x4352:
			case 0x4362:
			case 0x4372:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].AAddress &= 0xFF00;
				dma.dmabanks[d].AAddress |= Byte;
				return;

			case 0x4303:
			case 0x4313:
			case 0x4323:
			case 0x4333:
			case 0x4343:
			case 0x4353:
			case 0x4363:
			case 0x4373:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].AAddress &= 0xFF;
				dma.dmabanks[d].AAddress |= Byte << 8;
				return;

			case 0x4304:
			case 0x4314:
			case 0x4324:
			case 0x4334:
			case 0x4344:
			case 0x4354:
			case 0x4364:
			case 0x4374:
				if(cpu.InDMAorHDMA) return;
				dma.dmabanks[d=((Address >>> 4) & 0x7)].ABank = Byte;
				dma.HDMAMemPointers[d] = null;
				return;

			case 0x4305:
			case 0x4315:
			case 0x4325:
			case 0x4335:
			case 0x4345:
			case 0x4355:
			case 0x4365:
			case 0x4375:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].DMACount_Or_HDMAIndirectAddress &= 0xff00;
				dma.dmabanks[d].DMACount_Or_HDMAIndirectAddress |= Byte;
				dma.HDMAMemPointers[d] = null;
				return;
			case 0x4306:
			case 0x4316:
			case 0x4326:
			case 0x4336:
			case 0x4346:
			case 0x4356:
			case 0x4366:
			case 0x4376:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].DMACount_Or_HDMAIndirectAddress &= 0xff;
				dma.dmabanks[d].DMACount_Or_HDMAIndirectAddress |= Byte << 8;
				dma.HDMAMemPointers[d] = null;
				return;

			case 0x4307:
			case 0x4317:
			case 0x4327:
			case 0x4337:
			case 0x4347:
			case 0x4357:
			case 0x4367:
			case 0x4377:
				if(cpu.InDMAorHDMA) return;
				dma.dmabanks[d = ((Address >>> 4) & 0x7)].IndirectBank = Byte;
				dma.HDMAMemPointers[d] = null;
				return;

			case 0x4308:
			case 0x4318:
			case 0x4328:
			case 0x4338:
			case 0x4348:
			case 0x4358:
			case 0x4368:
			case 0x4378:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 7;
				dma.dmabanks[d].Address &= 0xff00;
				dma.dmabanks[d].Address |= Byte;
				HDMAMemPointers.put8Bit(d, 0);
				return;

			case 0x4309:
			case 0x4319:
			case 0x4329:
			case 0x4339:
			case 0x4349:
			case 0x4359:
			case 0x4369:
			case 0x4379:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				dma.dmabanks[d].Address &= 0xff;
				dma.dmabanks[d].Address |= Byte << 8;
				HDMAMemPointers.put8Bit(d, 0);
				return;

			case 0x430A:
			case 0x431A:
			case 0x432A:
			case 0x433A:
			case 0x434A:
			case 0x435A:
			case 0x436A:
			case 0x437A:
				if(cpu.InDMAorHDMA) return;
				d = (Address >>> 4) & 0x7;
				if( ( Byte & 0x7f ) > 0 )
				{
					dma.dmabanks[d].LineCount = Byte & 0x7f;
					dma.dmabanks[d].Repeat = ( Byte & 0x80 ) == 0;
				} else {
					dma.dmabanks[d].LineCount = 128;
					dma.dmabanks[d].Repeat = ( Byte & 0x80 ) != 0;
				}
				return;
			
			case 0x430B:
			case 0x431B:
			case 0x432B:
			case 0x433B:
			case 0x434B:
			case 0x435B:
			case 0x436B:
			case 0x437B:
			case 0x430F:
			case 0x431F:
			case 0x432F:
			case 0x433F:
			case 0x434F:
			case 0x435F:
			case 0x436F:
			case 0x437F:
				if(cpu.InDMAorHDMA) return;
				dma.dmabanks[((Address >>> 4) & 0x7)].UnknownByte = Byte;
				return;

			//These registers are used by both the S-DD1 and the SPC7110
			/*
			case 0x4800:
			case 0x4801:
			case 0x4802:
			case 0x4803:
				if(settings.SPC7110)
				{
					globals.s7r.SetSPC7110(Byte, Address);
				}
				break;

			case 0x4804:
			case 0x4805:
			case 0x4806:
			case 0x4807:
				if(settings.SPC7110)
				{
					globals.s7r.SetSPC7110(Byte, Address);
				}
				else
				{
					//TODO: SetSDD1MemoryMap (Address - 0x4804, Byte & 7);
				}
				break;

				//these are used by the SPC7110
			case 0x4808:
			case 0x4809:
			case 0x480A:
			case 0x480B:
			case 0x480C:
			case 0x4810:
			case 0x4811:
			case 0x4812:
			case 0x4813:
			case 0x4814:
			case 0x4815:
			case 0x4816:
			case 0x4817:
			case 0x4818:
			case 0x481A:
			case 0x4820:
			case 0x4821:
			case 0x4822:
			case 0x4823:
			case 0x4824:
			case 0x4825:
			case 0x4826:
			case 0x4827:
			case 0x4828:
			case 0x4829:
			case 0x482A:
			case 0x482B:
			case 0x482C:
			case 0x482D:
			case 0x482E:
			case 0x482F:
			case 0x4830:
			case 0x4831:
			case 0x4832:
			case 0x4833:
			case 0x4834:
			case 0x4840:
			case 0x4841:
			case 0x4842:
				if(settings.SPC7110)
				{
					globals.s7r.SetSPC7110(Byte, Address);
					break;
				}
			*/
			default:
				break;
			}
		memory.FillRAM.put8Bit(Address, Byte);
	}
	
	int GetCPU(int Address)
	{
		int d;
		int Byte;

		if (Address < 0x4200)
		{
			cpu.Cycles += SnesSystem.ONE_CYCLE;
			
			switch (Address)
			{
			case 0x4016:
			case 0x4017:
				return globals.controls.ReadJOYSERn(Address);
			default:

				return globals.OpenBus;
			}
		}
		else
		{
			switch (Address)
			{
			case 0x4200:
			case 0x4201:
			case 0x4202:
			case 0x4203:
			case 0x4204:
			case 0x4205:
			case 0x4206:
			case 0x4207:
			case 0x4208:
			case 0x4209:
			case 0x420a:
			case 0x420b:
			case 0x420c:
			case 0x420d:
			case 0x420e:
			case 0x420f:
				return globals.OpenBus;
	
			case 0x4210:
				cpu.WaitAddress = cpu.PBPCAtOpcodeStart;
				Byte = memory.FillRAM.getByte(0x4210);
				memory.FillRAM.put8Bit(0x4210, SnesSystem._5A22);
				//SNEeSe returns 2 for 5A22 version.
				return ( ( Byte & 0x80 ) | ( globals.OpenBus & 0x70 ) | SnesSystem._5A22 );
	
			case 0x4211:
				
				Byte = (cpu.IRQActive != 0 && ((PPU.PPU_V_BEAM_IRQ_SOURCE | PPU.PPU_H_BEAM_IRQ_SOURCE) > 0)) ? 0x80 : 0;
				
				// Super Robot Wars Ex ROM bug requires this.
				// Let's try without, now that we have Open Bus emulation?
				// byte |= CPU.Cycles >= Timings.HBlankStart ? 0x40 : 0;
				cpu.ClearIRQSource (PPU.PPU_V_BEAM_IRQ_SOURCE | PPU.PPU_H_BEAM_IRQ_SOURCE);
				Byte |= globals.OpenBus & 0x7f;
	
				return (Byte);
	
			case 0x4212:
				// V-blank, h-blank and joypads being read flags (read-only)
				cpu.WaitAddress = cpu.PBPCAtOpcodeStart;
	
				return ( REGISTER_4212() | (globals.OpenBus & 0x3E) );
	
			case 0x4213:
				// I/O port input - returns 0 wherever $4201 is 0, and 1 elsewhere
				// unless something else pulls it down (i.e. a gun)
				return memory.FillRAM.get8Bit(0x4213);
	
			case 0x4214:
			case 0x4215:
				// Quotient of divide result
			case 0x4216:
			case 0x4217:
				// Multiplcation result (for multiply) or remainder of
				// divison.
				return memory.FillRAM.get8Bit(Address);
			case 0x4218:
			case 0x4219:
			case 0x421a:
			case 0x421b:
			case 0x421c:
			case 0x421d:
			case 0x421e:
			case 0x421f:
	
				// Joypads 1-4 button and direction state.
				return memory.FillRAM.get8Bit(Address);
	
			case 0x4300:
			case 0x4310:
			case 0x4320:
			case 0x4330:
			case 0x4340:
			case 0x4350:
			case 0x4360:
			case 0x4370:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				d = (Address >>> 4) & 0x7;
				return ((dma.dmabanks[d].ReverseTransfer?0x80:0x00) |
						(dma.dmabanks[d].HDMAIndirectAddressing?0x40:0x00) |
						(dma.dmabanks[d].UnusedBit43x0?0x20:0x00) |
						(dma.dmabanks[d].AAddressDecrement?0x10:0x00) |
						(dma.dmabanks[d].AAddressFixed?0x08:0x00) |
						(dma.dmabanks[d].TransferMode & 7));
			case 0x4301:
			case 0x4311:
			case 0x4321:
			case 0x4331:
			case 0x4341:
			case 0x4351:
			case 0x4361:
			case 0x4371:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return dma.dmabanks[((Address >>> 4) & 0x7)].BAddress;
	
			case 0x4302:
			case 0x4312:
			case 0x4322:
			case 0x4332:
			case 0x4342:
			case 0x4352:
			case 0x4362:
			case 0x4372:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].AAddress & 0xFF);
	
			case 0x4303:
			case 0x4313:
			case 0x4323:
			case 0x4333:
			case 0x4343:
			case 0x4353:
			case 0x4363:
			case 0x4373:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].AAddress >>> 8);
	
			case 0x4304:
			case 0x4314:
			case 0x4324:
			case 0x4334:
			case 0x4344:
			case 0x4354:
			case 0x4364:
			case 0x4374:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return dma.dmabanks[((Address >>> 4) & 0x7)].ABank;
	
			case 0x4305:
			case 0x4315:
			case 0x4325:
			case 0x4335:
			case 0x4345:
			case 0x4355:
			case 0x4365:
			case 0x4375:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].DMACount_Or_HDMAIndirectAddress & 0xff);
	
			case 0x4306:
			case 0x4316:
			case 0x4326:
			case 0x4336:
			case 0x4346:
			case 0x4356:
			case 0x4366:
			case 0x4376:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].DMACount_Or_HDMAIndirectAddress >>> 8);
	
			case 0x4307:
			case 0x4317:
			case 0x4327:
			case 0x4337:
			case 0x4347:
			case 0x4357:
			case 0x4367:
			case 0x4377:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return dma.dmabanks[((Address >>> 4) & 0x7)].IndirectBank;
	
			case 0x4308:
			case 0x4318:
			case 0x4328:
			case 0x4338:
			case 0x4348:
			case 0x4358:
			case 0x4368:
			case 0x4378:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].Address & 0xFF);
	
			case 0x4309:
			case 0x4319:
			case 0x4329:
			case 0x4339:
			case 0x4349:
			case 0x4359:
			case 0x4369:
			case 0x4379:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return (dma.dmabanks[((Address >>> 4) & 0x7)].Address >>> 8);
	
			case 0x430A:
			case 0x431A:
			case 0x432A:
			case 0x433A:
			case 0x434A:
			case 0x435A:
			case 0x436A:
			case 0x437A:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				d = (Address >>> 4) & 0x7;
				return (dma.dmabanks[d].LineCount ^ ( dma.dmabanks[d].Repeat ? 0x00 : 0x80 ) );
	
			case 0x430B:
			case 0x431B:
			case 0x432B:
			case 0x433B:
			case 0x434B:
			case 0x435B:
			case 0x436B:
			case 0x437B:
			case 0x430F:
			case 0x431F:
			case 0x432F:
			case 0x433F:
			case 0x434F:
			case 0x435F:
			case 0x436F:
			case 0x437F:
				if(cpu.InDMAorHDMA) return globals.OpenBus;
				return dma.dmabanks[((Address >>> 4) & 0x7)].UnknownByte;
	
			default:
	
				/*
				if( Address >= 0x4800 && settings.SPC7110)
				{
					return globals.s7r.GetSPC7110(Address);
				}
	
				if( Address >= 0x4800 && Address <= 0x4807 && settings.SDD1 )
				{
					return memory.FillRAM.get8Bit(Address);
				}
				*/
	
				return globals.OpenBus;
			}
		}
	}

	void ResetPPU()
	{
	    SoftResetPPU();
	    globals.controls.ControlsReset();
	    PreviousLine = CurrentLine = 0;
		M7HOFS = 0;
		M7VOFS = 0;
		M7byte = 0;
	}

	void SoftResetPPU()
	{
		globals.controls.ControlsSoftReset();
		BGMode = 0;
		BG3Priority = false;
		Brightness = 0;
		VMA_High = false;
		VMA_Increment = 1;
		VMA_Address = 0;
		VMA_FullGraphicCount = 0;
		VMA_Shift = 0;

		for (int B = 0; B != 4; B++)
		{
			BG[B] = new Bg();
			BG[B].TileMapAddress = 0;
			BG[B].VOffset = 0;
			BG[B].HOffset = 0;
			BG[B].TileSizeMode = 0;
			BG[B].TileDataBase = 0;
			BG[B].TileMapSize = 0;

			ClipCounts[B] = 0;
			ClipWindowOverlapLogic [B] = PPU.CLIP_OR;
			ClipWindow1Enable[B] = false;
			ClipWindow2Enable[B] = false;
			ClipWindow1Inside[B] = true;
			ClipWindow2Inside[B] = true;
		}

		ClipCounts[4] = 0;
		ClipCounts[5] = 0;
		ClipWindowOverlapLogic[4] = ClipWindowOverlapLogic[5] = PPU.CLIP_OR;
		ClipWindow1Enable[4] = ClipWindow1Enable[5] = false;
		ClipWindow2Enable[4] = ClipWindow2Enable[5] = false;
		ClipWindow1Inside[4] = ClipWindow1Inside[5] = true;
		ClipWindow2Inside[4] = ClipWindow2Inside[5] = true;

		CGFLIP = false;
		
		for (int c = 0; c < CGDATA.length; c++)
		{
			CGDATA[c] = (short)( ( ( c & 7 ) << 2 ) | ( ( ( c >> 3 ) & 7 ) << 7 ) | ( ( ( c >> 6 ) & 2 ) << 13) );
		}

		FirstSprite = 0;
		LastSprite = 127;
		for (int Sprite = 0; Sprite < 128; Sprite++)
		{
			OBJ[Sprite] = new SOBJ();
			OBJ[Sprite].HPos = 0;
			OBJ[Sprite].VPos = 0;
			OBJ[Sprite].VH_Flip = 0;
			OBJ[Sprite].Priority = 0;
			OBJ[Sprite].Palette = 0;
			OBJ[Sprite].Name = 0;
			OBJ[Sprite].Size = 0;
			OBJ[Sprite].Changed = true;
		}
		
		OAMPriorityRotation = false;
		OAMWriteRegister = 0;
		RangeTimeOver = 0;
		OpenBus1 = 0;
		OpenBus2 = 0;

		OAMFlip = 0;
		OAMTileAddress = 0;
		OAMAddr = 0;
		IRQVBeamPos = 0x1ff;
		IRQHBeamPos = 0x1ff;
		VBeamPosLatched = 0;
		HBeamPosLatched = 0;

		HBeamFlip = 0;
		VBeamFlip = 0;
		HVBeamCounterLatched = 0;

		MatrixA = MatrixB = MatrixC = MatrixD = 0;
		CentreX = CentreY = 0;
		CGADD = 0;
		FixedColourRed = FixedColourGreen = FixedColourBlue = 0;
		SavedOAMAddr = 0;
		ScreenHeight = SnesSystem.SNES_HEIGHT;
		WRAM = 0;
		ForcedBlanking = true;
		OBJThroughMain = false;
		OBJThroughSub = false;
		OBJSizeSelect = 0;
		OBJNameSelect = 0;
		OBJNameBase = 0;
		OBJAddition = false;
		OAMReadFlip = 0;
		BGnxOFSbyte = 0;
		OAMData.zero();

		VTimerEnabled = false;
		HTimerEnabled = false;
		HTimerPosition = timings.H_Max + 1;
		VTimerPosition = timings.V_Max + 1;
		Mosaic = 0;
		BGMosaic[0] = BGMosaic[1] = false;
		BGMosaic[2] = BGMosaic[3] = false;
		Mode7HFlip = false;
		Mode7VFlip = false;
		Mode7Repeat = 0;
		Window1Left = 1;
		Window1Right = 0;
		Window2Left = 1;
		Window2Right = 0;
		RecomputeClipWindows = true;
		CGFLIPRead = 0;
		Need16x8Mulitply = false;

		ColorsChanged = true;
		HDMA = 0;
		HDMAEnded = 0;
		OBJChanged = true;
		RenderThisFrame = true;

		FrameCount = 0;
		RenderedFramesCount = 0;
		ZeroTileCache();
		VRAMReadBuffer = 0;
		Interlace = false;
		InterlaceOBJ = false;
		DoubleWidthPixels = false;
		DoubleHeightPixels = false;
		RenderedScreenWidth = SnesSystem.SNES_WIDTH;
		RenderedScreenHeight = SnesSystem.SNES_HEIGHT;

		PreviousLine = CurrentLine = 0;

		for (int c = 0; c < Clip.length; c++)
		{
			PPU.ClipData[] tmp = Clip[c];
			
			for( int j = 0; j < tmp.length; j++)
			{
				PPU.ClipData clip = new ClipData();
				clip.zero();
				Clip[c][j] = clip; 
			}
		}

		for (int c = 0; c < 0x8000; c += 0x100)
		{
			memory.FillRAM.fill(c, c >>> 8, 0x100);
		}

		memory.FillRAM.fill(0, 0x2100, 0x100);
		memory.FillRAM.fill(0, 0x4200, 0x100);
		memory.FillRAM.fill(0, 0x4000, 0x100);
		// For BS Suttehakkun 2...
		memory.FillRAM.fill(0, 0x1000, 0x1000);

		memory.FillRAM.put8Bit(0x4201, 0xFF);
		memory.FillRAM.put8Bit(0x4213, 0xFF);
	}
	
	final void FLUSH_REDRAW ()
	{
		/*
		if (PreviousLine != CurrentLine)
		{
			globals.GFX.UpdateScreen();
		}
		*/
	}

	private final void REGISTER_2118 (int Byte)
	{		
		if ( CHECK_INBLANK() ) return;
		
		int address;
		
		if (VMA_FullGraphicCount > 0)
		{
			int rem = VMA_Address & VMA_Mask1;
			address = (((VMA_Address & ~VMA_Mask1) +
					 (rem >>> VMA_Shift) +
					 ((rem & (VMA_FullGraphicCount - 1)) << 3)) << 1) & 0xffff;
		}
		else
		{
			address = (VMA_Address << 1) & 0xFFFF;
		}
		
		if (SnesSystem.DEBUG_PPU)
			System.out.format("REGISTER_2118 address = %04x, byte = %02x\n", address, Byte);
				
		memory.VRAM.put8Bit(address, Byte);
		
		RegisterTileCache(address);
		
		if (!VMA_High)
		{
			VMA_Address += VMA_Increment;
		}

	}
	
	private final void REGISTER_2119 (int Byte)
	{		
		if ( CHECK_INBLANK() ) return;

		int address;
		if (VMA_FullGraphicCount > 0)
		{
			int rem = VMA_Address & VMA_Mask1;
			address = ((((VMA_Address & ~VMA_Mask1) +
						(rem >>> VMA_Shift) +
						((rem & (VMA_FullGraphicCount - 1)) << 3)) << 1) + 1) & 0xFFFF;
		}
		else
		{
			address = ((VMA_Address << 1) + 1) & 0xFFFF;
		}
		
		if (SnesSystem.DEBUG_PPU)
			System.out.format("REGISTER_2119 address = %04x, byte = %02x\n", address, Byte);
		
		memory.VRAM.put8Bit(address, Byte);
		
		RegisterTileCache(address);

		if (VMA_High)
		{
			VMA_Address += VMA_Increment;
		}
	}

	final void REGISTER_2118_tile (int Byte)
	{		
		if ( CHECK_INBLANK() ) return;

		int rem = VMA_Address & VMA_Mask1;
		int address = (((VMA_Address & ~VMA_Mask1) +
			 (rem >>> VMA_Shift) +
			 ((rem & (VMA_FullGraphicCount - 1)) << 3)) << 1) & 0xffff;
		
		memory.VRAM.put8Bit(address, Byte);
		
		if (SnesSystem.DEBUG_PPU)
			System.out.format("REGISTER_2118_tile address = %04x, byte = %02x\n", address, Byte);
		
		RegisterTileCache(address);
		
		if ( ! VMA_High)
		{
			VMA_Address += VMA_Increment;
		}
	}
	
	final void REGISTER_2119_tile (int Byte)
	{

		if ( CHECK_INBLANK() ) return;

		int rem = VMA_Address & VMA_Mask1;
		int address = ((((VMA_Address & ~VMA_Mask1) +
				(rem >>> VMA_Shift) +
				((rem & (VMA_FullGraphicCount - 1)) << 3)) << 1) + 1) & 0xFFFF;

		if (SnesSystem.DEBUG_PPU)
			System.out.format("REGISTER_2119_tile address = %04x, byte = %02x\n", address, Byte);
		
		memory.VRAM.put8Bit(address, Byte);
		
		RegisterTileCache(address);
		
		if (VMA_High)
		{
			VMA_Address += VMA_Increment;
		}
	}

	final void REGISTER_2118_linear (int Byte)
	{		
		if ( CHECK_INBLANK() ) return;

		int address = (VMA_Address << 1) & 0xFFFF;
		
		memory.VRAM.put8Bit(address, Byte);
		
		if ( SnesSystem.DEBUG_PPU )
			System.out.format("REGISTER_2118_linear address = %04x, byte = %02x\n", address, Byte);
		
		RegisterTileCache(address);
		
		if ( ! VMA_High)
		{
			VMA_Address += VMA_Increment;
		}
	}
	
	final void REGISTER_2119_linear (int Byte)
	{		
		if ( CHECK_INBLANK() ) return;

		int address = ((VMA_Address << 1) + 1) & 0xFFFF;
		
		if ( SnesSystem.DEBUG_PPU )
			System.out.format("REGISTER_2119_linear address = %04x, byte = %02x\n", address, Byte);
		
		memory.VRAM.put8Bit(address, Byte);
		
		RegisterTileCache(address);
		
		if ( VMA_High )
		{
			VMA_Address += VMA_Increment;
		}
	}
	
	private void RegisterTileCache(int address)
	{
		TileCached[PPU.TILE_2BIT][address >>> 4] = 0;
		TileCached[PPU.TILE_4BIT][address >>> 5] = 0;
		TileCached[PPU.TILE_8BIT][address >>> 6] = 0;
		/*
		TileCached[SPPU.TILE_2BIT_EVEN][address >>> 4] = 0;
		TileCached[SPPU.TILE_2BIT_EVEN][( ( address >>> 4 ) - 1 ) & ( SPPU.MAX_2BIT_TILES - 1 )] = 0;
		TileCached[SPPU.TILE_2BIT_ODD][address >>> 4] = 0;
		TileCached[SPPU.TILE_2BIT_ODD][( ( address >>> 4 ) - 1 ) & ( SPPU.MAX_2BIT_TILES - 1 )] = 0;
		TileCached[SPPU.TILE_4BIT_EVEN][address >>> 5] = 0;
		TileCached[SPPU.TILE_4BIT_EVEN][( ( address >>> 5 ) - 1 ) & ( SPPU.MAX_4BIT_TILES - 1 )] = 0;
		TileCached[SPPU.TILE_4BIT_ODD][address >>> 5] = 0;
		TileCached[SPPU.TILE_4BIT_ODD][( ( address >>> 5 ) - 1 ) & ( SPPU.MAX_4BIT_TILES - 1 )] = 0;
		*/		
	}

	void REGISTER_2122(int Byte)
	{		
		if (CGFLIP)
		{
			if ( (Byte & 0x7f) != (CGDATA[CGADD] >>> 8) )
			{
				// FLUSH_REDRAW();
				
				int color = CGDATA[CGADD] & 0xF9C0;
				color |= (0x3 & Byte) << 9;
				color |= (0x7C & Byte) >> 1;
				CGDATA[CGADD] = (short) color;
				
				ColorsChanged = true;
				
				//PaletteCached[CGADD >>> 4] = 0;
				
				if (SnesSystem.DEBUG_PPU_MODES)
					System.out.format("ColorsChanged [ %d ] = %04x\n", CGADD, CGDATA[CGADD] );

			}
			CGADD = ( CGADD + 1 ) & 0xFF;
		}
		else if (Byte != (CGDATA[CGADD] & 0xff) )
		{
			//FLUSH_REDRAW();
			
			int color = CGDATA[CGADD] & 0x63E;
			color |= (Byte & 0x1F ) << 11;
			color |= (Byte & 0xE0 ) << 1;
			CGDATA[CGADD] = (short) color;
		}
		
		CGFLIP = CGFLIP ^ true;
	}

	void REGISTER_2180(int Byte)
	{		
		memory.RAM.put8Bit(WRAM++, Byte);
		WRAM &= 0x1FFFF;
		memory.FillRAM.put8Bit(0x2180, Byte);
	}
	
	private int REGISTER_4212()
	{
	    globals.GetBank = 0;
	    if (cpu.V_Counter >= ScreenHeight + PPU.FIRST_VISIBLE_LINE &&
		cpu.V_Counter < ScreenHeight + PPU.FIRST_VISIBLE_LINE + 3)
	    	globals.GetBank = 1;

	    globals.GetBank |= ((cpu.Cycles < timings.HBlankEnd) || (cpu.Cycles >= timings.HBlankStart)) ? 0x40 : 0;
	    
	    if (cpu.V_Counter >= ScreenHeight + PPU.FIRST_VISIBLE_LINE)
	    {
	    	globals.GetBank |= 0x80; /* XXX: 0x80 or 0xc0 ? */
	    }

	    return (globals.GetBank);
	}
	
	private boolean CHECK_INBLANK()
	{				
		return ( ! ForcedBlanking ) && cpu.V_Counter < (ScreenHeight + PPU.FIRST_VISIBLE_LINE);
	}	

	void REGISTER_2104(int Byte)
	{
	    if ( ( OAMAddr & 0x100 ) > 0 )
	    {
	        int addr = ((OAMAddr & 0x10f) << 1) + (OAMFlip & 1);
	        
	        if (Byte != OAMData.get8Bit(addr) )
	        {
	            FLUSH_REDRAW ();
	            OAMData.put8Bit(addr, Byte);
	            OBJChanged = true;

	            // X position high bit, and sprite size (x4)
	            int obj_position = (addr & 0x1f) * 4;
	            
	            // NAC: Verify SignExtend.
	            SOBJ pObj = OBJ[obj_position];
	            pObj.HPos = (pObj.HPos & 0xFF) | Globals.SignExtend[(Byte >>> 0) & 1];
	            pObj.Size = (Byte & 2) << 2;
	            pObj.Changed = true;

	            pObj = OBJ[obj_position++];
	            pObj.HPos = (pObj.HPos & 0xFF) | Globals.SignExtend[(Byte >>> 2) & 1];
	            pObj.Size = (Byte & 8);
	            pObj.Changed = true;
	            
	            pObj = OBJ[obj_position++];
	            pObj.HPos = (pObj.HPos & 0xFF) | Globals.SignExtend[(Byte >>> 4) & 1];
	            pObj.Size = (Byte & 32) >> 2;
	            pObj.Changed = true;
	            
	            pObj = OBJ[obj_position++];
	            pObj.HPos = (pObj.HPos & 0xFF) | Globals.SignExtend[(Byte >>> 6) & 1];
	            pObj.Size = (Byte & 128) >> 4;
	            pObj.Changed = true;
	            
	        }
	        
	        OAMFlip ^= 1;
	        
			if( (OAMFlip & 1) == 0)
			{
				++OAMAddr;
				OAMAddr &= 0x1ff;
				if ( OAMPriorityRotation && FirstSprite != (OAMAddr >>> 1) )
				{
					FirstSprite = (OAMAddr & 0xFE) >>> 1;
					OBJChanged = true;
				}
			}
			else
			{
				if( OAMPriorityRotation && ( OAMAddr & 1 ) == 1 )
					OBJChanged = true;
			}
			
	    }
	    else if( (OAMFlip & 1) == 0 )
	    {
	        OAMWriteRegister &= 0xff00;
	        OAMWriteRegister |= Byte;
	        OAMFlip |= 1;
	        
			if ( OAMPriorityRotation && (OAMAddr & 1 ) == 1 )
			{
				OBJChanged = true;
			}
	    }
	    else
	    {
	        OAMWriteRegister &= 0x00ff;
	        int lowByte = (OAMWriteRegister) & 0xFF;
	        int highByte = Byte;
	        OAMWriteRegister |= Byte << 8;

	        int addr = (OAMAddr << 1);

	        if (lowByte != OAMData.get8Bit(addr) || highByte != OAMData.get8Bit(addr + 1) )
	        {
	            FLUSH_REDRAW ();
	            OAMData.put8Bit(addr, lowByte);
	            OAMData.put8Bit(addr + 1, highByte);
	            OBJChanged = true;
	            
	            if ( ( addr & 2 ) == 2)
	            {
	                // Tile
	                OBJ[addr = OAMAddr >>> 1].Name = OAMWriteRegister & 0x1ff;

	                // priority, h and v flip.
	                OBJ[addr].Palette = (highByte >>> 1) & 7;
	                OBJ[addr].Priority = (byte) ((highByte >>> 4) & 3);
	                OBJ[addr].VH_Flip = (byte) (highByte >>> 6);
	                OBJ[addr].Changed = true;
	            }
	            else
	            {
	                // X position (low)
	                OBJ[addr = OAMAddr >>> 1].HPos &= 0xFF00;
	                OBJ[addr].HPos |= lowByte;

	                // Sprite Y position
	                OBJ[addr].VPos = highByte;
	                OBJ[addr].Changed = true;
	            }
	        }
	        OAMFlip &= ~1;
	        ++OAMAddr;
	        
			if ( OAMPriorityRotation && FirstSprite != (OAMAddr >>> 1) )
			{
				FirstSprite = (OAMAddr & 0xFE) >>> 1;
				OBJChanged = true;
			}
	    }

	    memory.FillRAM.put8Bit(0x2104, Byte);
	}
	
	void SuperFXExec()
	{

		if (settings.SuperFX)
		{
			if ((memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_SFR) & SuperFX.FLG_G) > 0 &&
				(memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_SCMR) & 0x18) == 0x18)
			{
				superfx.FxEmulate((memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_CLSR) & 1) == 1 ? superfx.speedPerLine * 2 : superfx.speedPerLine);
				
				int GSUStatus = memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_SFR) |
						(memory.FillRAM.get8Bit(0x3000 + SuperFX.GSU_SFR + 1) << 8);
				if ((GSUStatus & (SuperFX.FLG_G | SuperFX.FLG_IRQ)) == SuperFX.FLG_IRQ)
				{
					// Trigger a GSU IRQ.
					cpu.SetIRQ( PPU.GSU_IRQ_SOURCE );
				}
			}
		}
		
		int tmp = (memory.FillRAM.get8Bit(0x3034) << 16) + memory.FillRAM.get16Bit(0x301e);

		if (tmp == -1)
		{
			// NAC: This must be some type of core dump
			/*
			while ( ( Memory.FillRAM.getByte(0x3030) & 0x20 ) != 0 )
			{
				int i;
				int vError;
				int avReg[] = new int[0x40];
				byte tmpa[] = new byte[128];
				int vPipe;
				int vColr;
				int vPor;
	
				superfx.FxPipeString(tmpa);
				
				// Make the string 32 chars long 
				if(strlen(tmpa) < 32)
				{ 
					memset(&tmp[strlen(tmpa)],' ',32-strlen(tmpa)); tmpa[32] = 0; 
				}
	
				// Copy registers (so we can see if any changed)
				vColr = superfx.FxGetColorRegister();
				vPor = superfx.FxGetPlotOptionRegister();
				
				memcpy(avReg,SuperFX.pvRegisters,0x40);
	
				// Print the pipe string
				// printf(tmp);
	
				// Execute the instruction in the pipe
				vPipe = superfx.vPipe;
				vError = superfx.FxEmulate(1);
	
				// Check if any registers changed (and print them if they did)
				for(i=0; i<16; i++)
				{
					int a = 0;
					int r1 = ((int)avReg[i*2]) | (((int)avReg[(i*2)+1])<<8);
					int r2 = (int)(SuperFX.pvRegisters[i*2]) | (((int)SuperFX.pvRegisters[(i*2)+1])<<8);
					
					if(i==15)
					{
						a = OPCODE_BYTES(vPipe);
					}
					
					if(((r1+a)&0xffff) != r2)
					{
						printf(" r%d=$%04x",i,r2);
					}
				}

				// Check SFR 
				int r1 = ((int)avReg[0x30]) | (((int)avReg[0x31])<<8);
				int r2 = (int)(SuperFX.pvRegisters[0x30]) | (((int)SuperFX.pvRegisters[0x31])<<8);
				if((r1&(1<<1)) != (r2&(1<<1)))
					printf(" Z=%d",(int)(!!(r2&(1<<1))));
				if((r1&(1<<2)) != (r2&(1<<2)))
					printf(" CY=%d",(int)(!!(r2&(1<<2))));
				if((r1&(1<<3)) != (r2&(1<<3)))
					printf(" S=%d",(int)(!!(r2&(1<<3))));
				if((r1&(1<<4)) != (r2&(1<<4)))
					printf(" OV=%d",(int)(!!(r2&(1<<4))));
				if((r1&(1<<5)) != (r2&(1<<5)))
					printf(" G=%d",(int)(!!(r2&(1<<5))));
				if((r1&(1<<6)) != (r2&(1<<6)))
					printf(" R=%d",(int)(!!(r2&(1<<6))));
				if((r1&(1<<8)) != (r2&(1<<8)))
					printf(" ALT1=%d",(int)(!!(r2&(1<<8))));
				if((r1&(1<<9)) != (r2&(1<<9)))
					printf(" ALT2=%d",(int)(!!(r2&(1<<9))));
				if((r1&(1<<10)) != (r2&(1<<10)))
					printf(" IL=%d",(int)(!!(r2&(1<<10))));
				if((r1&(1<<11)) != (r2&(1<<11)))
					printf(" IH=%d",(int)(!!(r2&(1<<11))));
				if((r1&(1<<12)) != (r2&(1<<12)))
					printf(" B=%d",(int)(!!(r2&(1<<12))));
				if((r1&(1<<15)) != (r2&(1<<15)))
					printf(" IRQ=%d",(int)(!!(r2&(1<<15))));

				// Check PBR
				int r1 = ((int)avReg[0x34]);
				int r2 = (int)(SuperFX.pvRegisters[0x34]);
				if(r1 != r2)
					printf(" PBR=$%02x",r2);

				// Check ROMBR
				int r1 = ((int)avReg[0x36]);
				int r2 = (int)(SuperFX.pvRegisters[0x36]);
				if(r1 != r2)
					printf(" ROMBR=$%02x",r2);

				// Check RAMBR
				int r1 = ((int)avReg[0x3c]);
				int r2 = (int)(SuperFX.pvRegisters[0x3c]);
				if(r1 != r2)
					printf(" RAMBR=$%02x",r2);

				// Check CBR 
				int r1 = ((int)avReg[0x3e]) | (((int)avReg[0x3f])<<8);
				int r2 = (int)(SuperFX.pvRegisters[0x3e]) | (((int)SuperFX.pvRegisters[0x3f])<<8);
				
				if(r1 != r2)
					printf(" CBR=$%04x",r2);

				// Check COLR 
				if(vColr != FxGetColorRegister())
					printf(" COLR=$%02x",FxGetColorRegister());

				// Check POR 
				if(vPor != FxGetPlotOptionRegister())
					printf(" POR=$%02x",FxGetPlotOptionRegister());

			}
			 */
			SnesSystem.Exit();
		}
		else
		{
			memory.FillRAM.buffer[0x3030] &= ~0x20;
			
			if ( ( memory.FillRAM.getByte(0x3031) & 0x80 ) != 0 )
			{
				cpu.SetIRQ(PPU.GSU_IRQ_SOURCE);
			}
		}
	}
	/*
	private void RegisterBGHorizontalChange( int BGNumber )
	{

		short bgScanLines[] = BGScanLinePositions[BGNumber];
		
		short LastEntry = bgScanLines[0];
		
		// If the vertical position has changed
		if ( BG[BGNumber].HOffset != bgScanLines[ LastEntry + PPU.BGIND_HORIZONTAL ] )
		{
			if (CurrentLine == (ScreenHeight - 1 )) return; // Stop changes from being recorded after end of screen
			
			// If the scan line number has changed
			if (bgScanLines[LastEntry] != CurrentLine)
			{
				LastEntry += 3;
				bgScanLines[0] = LastEntry;
				
				bgScanLines[LastEntry] = (short) CurrentLine;
				bgScanLines[LastEntry + PPU.BGIND_HORIZONTAL] = BG[BGNumber].HOffset;
				bgScanLines[LastEntry + PPU.BGIND_VERTICAL] = (short) (BG[BGNumber].VOffset + 1);
			}
			// Just update the current line
			else
			{
				bgScanLines[LastEntry + PPU.BGIND_HORIZONTAL] = BG[BGNumber].HOffset;
			}
		}
	}

	private void RegisterBGVerticalChange( int BGNumber )
	{
		short bgScanLines[] = BGScanLinePositions[BGNumber];
		
		short LastEntry = bgScanLines[0];
		
		// If the vertical position has changed
		if ( BG[BGNumber].VOffset != bgScanLines[ LastEntry + PPU.BGIND_VERTICAL ] )
		{
			if (CurrentLine == (ScreenHeight - 1 )) return; // Stop changes from being recorded after end of screen

			// If the scan line number has changed
			if (bgScanLines[LastEntry] != CurrentLine)
			{
				LastEntry += 3;
				bgScanLines[0] = LastEntry;
				
				bgScanLines[LastEntry] = (short) CurrentLine;
				bgScanLines[LastEntry + PPU.BGIND_HORIZONTAL] = BG[BGNumber].HOffset;
				bgScanLines[LastEntry + PPU.BGIND_VERTICAL] = (short) (BG[BGNumber].VOffset + 1);
			}
			// Just update the current line
			else
			{
				bgScanLines[LastEntry + PPU.BGIND_VERTICAL] = (short) (BG[BGNumber].VOffset + 1);
			}
		}
	}
	
	void ResetBGPositionTracking()
	{
		for(int i = 0; i < 4; i++)
		{
			BGScanLinePositions[i][PPU.BGIND_SCAN_LINE] = 0;
			BGScanLinePositions[i][PPU.BGIND_HORIZONTAL] = BG[i].HOffset;
			BGScanLinePositions[i][PPU.BGIND_VERTICAL] = (short) (BG[i].VOffset + 1);
		}

	}
	
	void RenderLine(int line)
	{
		CurrentLine = line;
	}
	*/
	
	private void BackgroundChanged()
	{
		if ( ( memory.FillRAM.getByte(0x2131) & 0x10) != 0 )
		{
			int color = CGDATA[0];
			GameCanvas.setBackgroundColor(
				( ( ( ( color & 0x1F ) + 1 ) << 3) - 1) * Brightness,
				( ( ( ( ( color >> 5 ) & 0x1F ) + 1 ) << 3) - 1) * Brightness,
				( ( ( ( ( color >> 10 ) & 0x1F ) + 1 ) << 3) - 1) * Brightness
			);
		}
		else
		{
			GameCanvas.setBackgroundColor(
				( ( ( FixedColourRed + 1 ) << 3) - 1) * Brightness,
				( ( ( FixedColourGreen + 1 ) << 3) - 1) * Brightness,
				( ( ( FixedColourBlue + 1 ) << 3) - 1) * Brightness
			);			
		}
	}
}