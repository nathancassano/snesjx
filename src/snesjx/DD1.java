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

class DD1
{
	static final int MEMMAP_MAX_SDD1_LOGGED_ENTRIES = 0x10000 / 8;

    private ByteArray SDD1Index;
    private ByteArray SDD1Data;
    private long DD1Entries;
    private long DD1LoggedDataCountPrev;
    private long SDD1LoggedDataCount;
    private ByteArray SDD1LoggedData = new ByteArray(MEMMAP_MAX_SDD1_LOGGED_ENTRIES);
    
	private int valid_bits;
	private int in_stream;
	private ByteArrayOffset in_buf;
	private int bit_ctr[] = new int[8];
	private int context_states[] = new int[32];
	private int context_MPS[] = new int[32];
	private int bitplane_type;
	private int high_context_bits;
	private int low_context_bits;
	private int prev_bits[] = new int[8];

	private int cur_plane;
	private int num_bits;
	private int next_byte;

	private static final int code_size = 0;
	private static final int MPS_next = 1;
	private static final int LPS_next = 2;
	
	private static final int evolution_table[][] = {
		/*	0 */ { 0,25,25},
		/*	1 */ { 0, 2, 1},
		/*	2 */ { 0, 3, 1},
		/*	3 */ { 0, 4, 2},
		/*	4 */ { 0, 5, 3},
		/*	5 */ { 1, 6, 4},
		/*	6 */ { 1, 7, 5},
		/*	7 */ { 1, 8, 6},
		/*	8 */ { 1, 9, 7},
		/*	9 */ { 2,10, 8},
		/* 10 */ { 2,11, 9},
		/* 11 */ { 2,12,10},
		/* 12 */ { 2,13,11},
		/* 13 */ { 3,14,12},
		/* 14 */ { 3,15,13},
		/* 15 */ { 3,16,14},
		/* 16 */ { 3,17,15},
		/* 17 */ { 4,18,16},
		/* 18 */ { 4,19,17},
		/* 19 */ { 5,20,18},
		/* 20 */ { 5,21,19},
		/* 21 */ { 6,22,20},
		/* 22 */ { 6,23,21},
		/* 23 */ { 7,24,22},
		/* 24 */ { 7,24,23},
		/* 25 */ { 0,26, 1},
		/* 26 */ { 1,27, 2},
		/* 27 */ { 2,28, 4},
		/* 28 */ { 3,29, 8},
		/* 29 */ { 4,30,12},
		/* 30 */ { 5,31,16},
		/* 31 */ { 6,32,18},
		/* 32 */ { 7,24,22}
	};

	private int run_table[] = {
	  128, 64, 96, 32, 112, 48, 80, 16, 120, 56, 88, 24, 104, 40, 72,
	    8, 124, 60, 92, 28, 108, 44, 76, 12, 116, 52, 84, 20, 100, 36,
	   68, 4, 126, 62, 94, 30, 110, 46, 78, 14, 118, 54, 86, 22, 102,
	   38, 70, 6, 122, 58, 90, 26, 106, 42, 74, 10, 114, 50, 82, 18,
	   98, 34, 66, 2, 127, 63, 95, 31, 111, 47, 79, 15, 119, 55, 87,
	   23, 103, 39, 71, 7, 123, 59, 91, 27, 107, 43, 75, 11, 115, 51,
	   83, 19, 99, 35, 67, 3, 125, 61, 93, 29, 109, 45, 77, 13, 117,
	   53, 85, 21, 101, 37, 69, 5, 121, 57, 89, 25, 105, 41, 73, 9,
	  113, 49, 81, 17, 97, 33, 65, 1
	};

	private int GetCodeword(int bits)
	{
		int tmp;

		if( valid_bits == 0 )
		{
			in_buf.setOffset(in_buf.getOffset() + 1);
			in_stream |= in_buf.get8Bit(0);
			valid_bits=8;
		}

		in_stream <<= 1;
		valid_bits--;
		in_stream ^= 0x8000;
		if( ( in_stream & 0x8000) != 0 )
			return 0x80 + ( 1 << bits);
		
		tmp = ( in_stream >> 8 ) | ( 0x7f >> bits );
		in_stream <<= bits;
		valid_bits -= bits;
		
		if( valid_bits < 0 )
		{
			in_buf.setOffset(in_buf.getOffset() + 1);
			in_stream |= in_buf.get8Bit(0) << (-valid_bits);
			valid_bits += 8;
		}
		return run_table[tmp];
	}

	private int GolombGetBit(int code_size)
	{
		if( bit_ctr[code_size] == 0 )
			bit_ctr[code_size]=GetCodeword(code_size);
		
		bit_ctr[code_size]--;
		
		if( bit_ctr[code_size] == 0x80)
		{
			bit_ctr[code_size] = 0;
			return 2; /* secret code for 'last zero'. ones are always last. */
		}
		return (bit_ctr[code_size] == 0) ? 1 : 0;
	}

	private int ProbGetBit(int context)
	{
		int state=context_states[context];
		int bit = GolombGetBit( evolution_table[state][code_size] );

		if( ( bit & 1 ) == 1 )
		{
			context_states[context] = evolution_table[state][LPS_next];
			
			if( state < 2 )
			{
				context_MPS[context] ^= 1;
				return context_MPS[context]; /* just inverted, so just return it */
			} 
			else 
			{
				return context_MPS[context] ^ 1; /* we know bit is 1, so use a constant */
			}
		}
		else if( bit > 0 )
		{
			context_states[context] = evolution_table[state][MPS_next];
			/* zero here, zero there, no difference so drop through. */
		}
		
		return context_MPS[context]; /* we know bit is 0, so don't bother xoring */
	}

	private int GetBit(int cur_bitplane){
		int bit;

		bit = ProbGetBit( ( ( cur_bitplane & 1 ) << 4 )
						| ( (prev_bits[cur_bitplane] & high_context_bits ) >> 5 )
						| ( prev_bits[cur_bitplane] & low_context_bits ) );

		prev_bits[cur_bitplane] <<= 1;
		prev_bits[cur_bitplane] |= bit;
		return bit;
	}

	void SDD1_decompress(ByteArray out, ByteArrayOffset in, int len)
	{
		int bit, i, plane;
		int byte1, byte2;
		int out_pos = 0;

		if( len == 0 )
			len = 0x10000;

		bitplane_type = in.get8Bit(0) >> 6;

		switch( in.getByte(0) & 0x30)
		{
		case 0x00:
			high_context_bits = 0x01c0;
			low_context_bits = 0x0001;
			break;
		case 0x10:
			high_context_bits = 0x0180;
			low_context_bits = 0x0001;
			break;
		case 0x20:
			high_context_bits = 0x00c0;
			low_context_bits = 0x0001;
			break;
		case 0x30:
			high_context_bits = 0x0180;
			low_context_bits = 0x0003;
			break;
		}

		in_stream = ( in.get8Bit(0) << 11) | ( in.get8Bit(1) << 3 );
		valid_bits = 5;
		
		
		in_buf = in.getOffsetBuffer( 2 );
		
		int j;
		
		for(j = 0; j < bit_ctr.length; j++)
			bit_ctr[j] = 0;
		
		for( j= 0 ; j < context_states.length; j++)
			context_states[j] = 0;
		
		for( j= 0 ; j < context_MPS.length; j++)
			context_MPS[j] = 0;
		
		for( j= 0 ; j < prev_bits.length; j++)
			prev_bits[0] = 0;

		switch(bitplane_type)
		{
		case 0:
			while(true)
			{
				for( byte1 = byte2 = 0, bit = 0x80; bit != 0; bit >>= 1)
				{
					if( GetBit(0) != 0)
						byte1 |= bit;
					if( GetBit(1) != 0 )
						byte2 |= bit;
				}
				out.put8Bit(out_pos++, byte1);
				if( --len == 0 )
					return;
				out.put8Bit(out_pos++, byte2);
				if( --len == 0 )
					return;
			}
		case 1:
			i = plane = 0;
			while(true)
			{
				for( byte1 = byte2 = 0, bit = 0x80; bit != 0; bit >>= 1)
				{
					if( GetBit(plane) != 0 )
						byte1 |= bit;
					if( GetBit(plane + 1) != 0 )
						byte2 |= bit;
				}
				out.put8Bit(out_pos++, byte1);
				if( --len == 0)
					return;
				out.put8Bit(out_pos++, byte2);
				if( --len == 0 )
					return;
				if( ( i += 32) == 0 )
					plane = (plane + 2) & 7;
			}
		case 2:
			i = plane = 0;
			while( true )
			{
				for(byte1 = byte2 = 0, bit = 0x80; bit != 0; bit >>= 1)
				{
					if( GetBit( plane ) != 0 )
						byte1 |= bit;
					if( GetBit( plane + 1 ) != 0 )
						byte2 |= bit;
				}
				out.put8Bit(out_pos++, byte1);
				if( --len == 0 )
					return;
				out.put8Bit(out_pos++, byte2);
				if( --len == 0 )
					return;
				if( ( i += 32 ) == 0 )
					plane ^= 2;
			}
		case 3:
			do
			{
				for( byte1 = plane = 0, bit = 1; bit != 0; bit <<= 1, plane++ )
				{
					if( GetBit(plane) != 0 )
						byte1 |= bit;
				}
				out.put8Bit(out_pos++, byte1);
			}
			while(--len != 0);
			break;
		}
	}


	private void SDD1_init(ByteArrayOffset in )
	{
		bitplane_type= in.get8Bit(0) >> 6;

		switch( in.getByte(0) & 0x30 )
		{
		case 0x00:
			high_context_bits = 0x01c0;
			low_context_bits = 0x0001;
			break;
		case 0x10:
			high_context_bits = 0x0180;
			low_context_bits = 0x0001;
			break;
		case 0x20:
			high_context_bits = 0x00c0;
			low_context_bits = 0x0001;
			break;
		case 0x30:
			high_context_bits = 0x0180;
			low_context_bits = 0x0003;
			break;
		}

		in_stream = ( in.get8Bit(0) << 11 ) | ( in.get8Bit(0) << 3 );
		valid_bits = 5;
		in_buf = in.getOffsetBuffer( 2 );
		
		int j;
		
		for(j = 0; j < bit_ctr.length; j++)
			bit_ctr[j] = 0;
		
		for( j= 0 ; j < context_states.length; j++)
			context_states[j] = 0;
		
		for( j= 0 ; j < context_MPS.length; j++)
			context_MPS[j] = 0;
		
		for( j= 0 ; j < prev_bits.length; j++)
			prev_bits[0] = 0;

		cur_plane=0;
		num_bits=0;
	}

	private int SDD1_get_byte()
	{
		int bit;
		int Byte = 0;

		switch( bitplane_type )
		{
			case 0:
			num_bits += 16;
			
			if( ( num_bits & 0x10 ) != 0 )
			{
				next_byte = 0;
				for( bit = 0x80; bit != 0; bit >>= 1 )
				{
					if( GetBit(0) != 0 )
						Byte |= bit;
					if( GetBit(1) != 0 )
						next_byte |= bit;
				}
				return Byte;
			} else {
				return next_byte;
			}

			case 1:
			num_bits+=16;
			
			if( ( num_bits & 0x10 ) != 0 )
			{
				next_byte = 0;
				for( bit = 0x80; bit != 0; bit >>= 1 )
				{
					if(GetBit(cur_plane) != 0) Byte |= bit;
					if(GetBit(cur_plane + 1) != 0) next_byte |= bit;
				}
				return Byte;
			} else {
				if( num_bits == 0)
					cur_plane = (cur_plane + 2) & 7;
				return next_byte;
			}

			case 2:
			num_bits += 16;
			if( ( num_bits & 0x10 ) != 0 )
			{
				next_byte = 0;
				for( bit = 0x80; bit != 0; bit >>= 1 )
				{
					if(GetBit(cur_plane) != 0) Byte |= bit;
					if(GetBit(cur_plane + 1) != 0) next_byte |= bit;
				}
				return Byte;
			} else {
				if( num_bits == 0)
					cur_plane ^= 2;
				return next_byte;
			}

			case 3:
			for( cur_plane = 0, bit = 1; bit != 0; bit <<= 1, cur_plane++ )
			{
				if( GetBit(cur_plane) != 0 )
					Byte |= bit;
			}
			return Byte;

			default:
			/* should never happen */
			return 0;
		}
	}

	private void SetSDD1MemoryMap(int bank, int value)
	{
		Memory Memory = Globals.globals.memory;
		
		bank = 0xc00 + bank * 0x100;
		value = value * 1024 * 1024;

		int c;

		for (c = 0; c < 0x100; c += 16)
		{
			int block = Memory.ROM.getOffset() + value + (c << 12);

			for (int i = c; i < c + 16; i++)
			{
				Memory.Map [i + bank] = block;
			}
		}
	}

	private void ResetSDD1()
	{
		Memory Memory = Globals.globals.memory;
		
		Memory.FillRAM.fill(0, 0x4800, 4);
		
		for (int i = 0; i < 4; i++)
		{
			Memory.FillRAM.put8Bit(0x4804 + i,	i);
			SetSDD1MemoryMap (i, i);
		}
	}

	private void SDD1PostLoadState ()
	{
		Memory Memory = Globals.globals.memory;
		
		for (int i = 0; i < 4; i++)
			SetSDD1MemoryMap(i, Memory.FillRAM.get8Bit( 0x4804 + i ) );
	}

	private int CompareSDD1LoggedDataEntries(ByteArrayOffset p1, ByteArrayOffset p2)
	{
		int a1 = (p1.get8Bit(0) << 16) + (p1.get8Bit(1) << 8) + p1.get8Bit(2);
		int a2 = (p2.get8Bit(0) << 16) + (p2.get8Bit(1) << 8) + p2.get8Bit(2);

		return (a1 - a2);
	}

	private void SDD1SaveLoggedData()
	{
		//TODO: Finish SDD1SaveLoggedData()
		/*
		CMemory Memory = Globals.globals.Memory;
		
		if (Memory.SDD1LoggedDataCount != Memory.SDD1LoggedDataCountPrev)
		{
			qsort (Memory.SDD1LoggedData, Memory.SDD1LoggedDataCount, 8,
				CompareSDD1LoggedDataEntries);

			FILE *fs = fopen (GetFilename (".dat", PATCH_DIR), "wb");

			if (fs)
			{
				fwrite (Memory.SDD1LoggedData, 8,
					Memory.SDD1LoggedDataCount, fs);
				fclose (fs);
				chown (GetFilename (".dat", PATCH_DIR), getuid (), getgid ());
			}
			Memory.SDD1LoggedDataCountPrev = Memory.SDD1LoggedDataCount;
		}
		*/
	}

	private void SDD1LoadLoggedData ()
	{
		//TODO: Finish SDD1LoadLoggedData()
		/*
		FILE *fs = fopen (GetFilename (".dat", PATCH_DIR), "rb");

		Memory.SDD1LoggedDataCount = Memory.SDD1LoggedDataCountPrev = 0;

		if (fs)
		{
			int c = fread (Memory.SDD1LoggedData, 8,
						MEMMAP_MAX_SDD1_LOGGED_ENTRIES, fs);
	
			if (c != EOF)
				Memory.SDD1LoggedDataCount = Memory.SDD1LoggedDataCountPrev = c;
			
			fclose (fs);
		}
		*/
	}

	
}
