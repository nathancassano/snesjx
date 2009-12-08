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

class CPUDebug
{
	private static String Mnemonics[] = {
	    "BRK", "ORA", "COP", "ORA", "TSB", "ORA", "ASL", "ORA",
	    "PHP", "ORA", "ASL", "PHD", "TSB", "ORA", "ASL", "ORA",
	    "BPL", "ORA", "ORA", "ORA", "TRB", "ORA", "ASL", "ORA",
	    "CLC", "ORA", "INC", "TCS", "TRB", "ORA", "ASL", "ORA",
	    "JSR", "AND", "JSL", "AND", "BIT", "AND", "ROL", "AND",
	    "PLP", "AND", "ROL", "PLD", "BIT", "AND", "ROL", "AND",
	    "BMI", "AND", "AND", "AND", "BIT", "AND", "ROL", "AND",
	    "SEC", "AND", "DEC", "TSC", "BIT", "AND", "ROL", "AND",
	    "RTI", "EOR", "WDM", "EOR", "MVP", "EOR", "LSR", "EOR",
	    "PHA", "EOR", "LSR", "PHK", "JMP", "EOR", "LSR", "EOR",
	    "BVC", "EOR", "EOR", "EOR", "MVN", "EOR", "LSR", "EOR",
	    "CLI", "EOR", "PHY", "TCD", "JMP", "EOR", "LSR", "EOR",
	    "RTS", "ADC", "PER", "ADC", "STZ", "ADC", "ROR", "ADC",
	    "PLA", "ADC", "ROR", "RTL", "JMP", "ADC", "ROR", "ADC",
	    "BVS", "ADC", "ADC", "ADC", "STZ", "ADC", "ROR", "ADC",
	    "SEI", "ADC", "PLY", "TDC", "JMP", "ADC", "ROR", "ADC",
	    "BRA", "STA", "BRL", "STA", "STY", "STA", "STX", "STA",
	    "DEY", "BIT", "TXA", "PHB", "STY", "STA", "STX", "STA",
	    "BCC", "STA", "STA", "STA", "STY", "STA", "STX", "STA",
	    "TYA", "STA", "TXS", "TXY", "STZ", "STA", "STZ", "STA",
	    "LDY", "LDA", "LDX", "LDA", "LDY", "LDA", "LDX", "LDA",
	    "TAY", "LDA", "TAX", "PLB", "LDY", "LDA", "LDX", "LDA",
	    "BCS", "LDA", "LDA", "LDA", "LDY", "LDA", "LDX", "LDA",
	    "CLV", "LDA", "TSX", "TYX", "LDY", "LDA", "LDX", "LDA",
	    "CPY", "CMP", "REP", "CMP", "CPY", "CMP", "DEC", "CMP",
	    "INY", "CMP", "DEX", "WAI", "CPY", "CMP", "DEC", "CMP",
	    "BNE", "CMP", "CMP", "CMP", "PEI", "CMP", "DEC", "CMP",
	    "CLD", "CMP", "PHX", "STP", "JML", "CMP", "DEC", "CMP",
	    "CPX", "SBC", "SEP", "SBC", "CPX", "SBC", "INC", "SBC",
	    "INX", "SBC", "NOP", "XBA", "CPX", "SBC", "INC", "SBC",
	    "BEQ", "SBC", "SBC", "SBC", "PEA", "SBC", "INC", "SBC",
	    "SED", "SBC", "PLX", "XCE", "JSR", "SBC", "INC", "SBC"
	};

	private static int AddrModes[] = {
	  //0   1  2   3  4  5  6   7  8   9  A  B   C   D   E   F
	    3, 10, 3, 19, 6, 6, 6, 12, 0,  1,24, 0, 14, 14, 14, 17, //0
	    4, 11, 9, 20, 6, 7, 7, 13, 0, 16,24, 0, 14, 15, 15, 18, //1
	   14, 10,17, 19, 6, 6, 6, 12, 0,  1,24, 0, 14, 14, 14, 17, //2
	    4, 11, 9, 20, 7, 7, 7, 13, 0, 16,24, 0, 15, 15, 15, 18, //3
	    0, 10, 3, 19, 25,6, 6, 12, 0,  1,24, 0, 14, 14, 14, 17, //4
	    4, 11, 9, 20, 25,7, 7, 13, 0, 16, 0, 0, 17, 15, 15, 18, //5
	    0, 10, 5, 19, 6, 6, 6, 12, 0,  1,24, 0, 21, 14, 14, 17, //6
	    4, 11, 9, 20, 7, 7, 7, 13, 0, 16, 0, 0, 23, 15, 15, 18, //7
	    4, 10, 5, 19, 6, 6, 6, 12, 0,  1, 0, 0, 14, 14, 14, 17, //8
	    4, 11, 9, 20, 7, 7, 8, 13, 0, 16, 0, 0, 14, 15, 15, 18, //9
	    2, 10, 2, 19, 6, 6, 6, 12, 0,  1, 0, 0, 14, 14, 14, 17, //A
	    4, 11, 9, 20, 7, 7, 8, 13, 0, 16, 0, 0, 15, 15, 16, 18, //B
	    2, 10, 3, 19, 6, 6, 6, 12, 0,  1, 0, 0, 14, 14, 14, 17, //C
	    4, 11, 9,  9,27, 7, 7, 13, 0, 16, 0, 0, 22, 15, 15, 18, //D
	    2, 10, 3, 19, 6, 6, 6, 12, 0,  1, 0, 0, 14, 14, 14, 17, //E
	    4, 11, 9, 20,26, 7, 7, 13, 0, 16, 0, 0, 23, 15, 15, 18  //F
	};
	
	private static String OPrint()
	{
		String Line;
		
		Globals globals = Globals.globals;
		CPU cpu = globals.cpu;
		
	    int Opcode;
	    int Operant[] = new int[3];
	    int Word;
	    int Byte;
	    int SByte;
	    short SWord;
	    
	    int Bank = cpu.PB();
	    int Address = cpu.PCw;

	    int Cycles = cpu.Cycles;
	    int WaitAddress = cpu.WaitAddress;

	    Opcode = cpu.GetByte( (Bank << 16) + Address );
	    
	    if (Opcode == 0x10)
	    {
	    	Opcode = 0x10;
	    }
	    
	    Line = String.format( "$%02X:%04X %02X ", Bank, Address, Opcode).toString(); 
	    Operant[0] = cpu.GetByte ((Bank << 16) + Address + 1);
	    Operant[1] = cpu.GetByte ((Bank << 16) + Address + 2);
	    Operant[2] = cpu.GetByte ((Bank << 16) + Address + 3);

	    switch (AddrModes[Opcode])
	    {
	    case 0:
		//Implied
		    Line = String.format( "%s         %s", Line, Mnemonics[Opcode]).toString();
		break;
	    case 1:
		//Immediate[MemoryFlag]
		if (! cpu.CheckFlag (CPU.MemoryFlag) )
		{
		    //Accumulator 16 - Bit
			Line = String.format( "%s%02X %02X    %s #$%02X%02X",
				 Line,
				 Operant[0],
				 Operant[1],
				 Mnemonics[Opcode],
				 Operant[1],
				 Operant[0]).toString();
		}
		else
		{
		    //Accumulator 8 - Bit
			Line = String.format( "%s%02X       %s #$%02X",
				 Line,
				 Operant[0],
				 Mnemonics[Opcode],
				 Operant[0]).toString();
		}
		break;
	    case 2:
		//Immediate[IndexFlag]
		if (! cpu.CheckFlag ( CPU.IndexFlag ) )
		{
		    //X / Y 16 - Bit
			Line = String.format( "%s%02X %02X    %s #$%02X%02X",
				 Line,
				 Operant[0],
				 Operant[1],
				 Mnemonics[Opcode],
				 Operant[1],
				 Operant[0]).toString();
		}
		else
		{
		    //X / Y 8 - Bit
			Line = String.format( "%s%02X       %s #$%02X",
				 Line,
				 Operant[0],
				 Mnemonics[Opcode],
				 Operant[0]).toString();
		}
		break;
	    case 3:
		//Immediate[Always 8 - Bit]
		if ( true )
		{
		    //Always 8 - Bit
			Line = String.format( "%s%02X       %s #$%02X",
				 Line,
				 Operant[0],
				 Mnemonics[Opcode],
				 Operant[0]).toString();
		}
		break;
	    case 4:
		//Relative
		    Line = String.format( "%s%02X       %s $%02X",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		SByte = (byte) Operant[0];
		Word = Address;
		Word = (Word + SByte ) & 0xFFFF;
		Word = (Word + 2 ) & 0xFFFF;
		Line = String.format( "%-28s[$%04X]", Line, Word).toString();
		break;
	    case 5:
		//Relative Long
		    Line = String.format( "%s%02X %02X    %s $%02X%02X",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		SWord = (short)( (Operant[1] << 8) | Operant[0] );
		Word = Address;
		Word = ( Word + SWord ) & 0xFFFF;
		Word = ( Word  + 3 ) & 0xFFFF;
		Line = String.format( "%-28s[$%04X]", Line, Word).toString();
		break;
	    case 6:
		//Direct
		    Line = String.format( "%s%02X       %s $%02X",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Line = String.format( "%-28s[$00:%04X]", Line, Word).toString();
		break;
	    case 7:
		//Direct indexed (with x)
		    Line = String.format( "%s%02X       %s $%02X,x",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word = ( Word + cpu.X ) & 0xFFFF;
		Line = String.format( "%-28s[$00:%04X]", Line, Word).toString();
		break;
	    case 8:
		//Direct indexed (with y)
		    Line = String.format( "%s%02X       %s $%02X,y",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word += cpu.Y;
		Line = String.format( "%-28s[$00:%04X]", Line, Word).toString();
		break;
	    case 9:
		//Direct Indirect
		    Line = String.format( "%s%02X       %s ($%02X)",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 10:
		//Direct Indexed Indirect
		    Line = String.format( "%s%02X       %s ($%02X,x)",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word = ( Word + cpu.X ) & 0xFFFF;
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 11:
		//Direct Indirect Indexed
		    Line = String.format( "%s%02X       %s ($%02X),y",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word = cpu.GetWord (Word);
		Word += cpu.Y;
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 12:
		//Direct Indirect Long
		    Line = String.format( "%s%02X       %s [$%02X]",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Byte = cpu.GetByte (Word + 2);
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, Byte, Word).toString();
		break;
	    case 13:
		//Direct Indirect Indexed Long
		    Line = String.format( "%s%02X       %s [$%02X],y",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Byte = cpu.GetByte (Word + 2);
		Word = cpu.GetWord (Word);
		Word += cpu.Y;
		Line = String.format( "%-28s[$%02X:%04X]", Line, Byte, Word).toString();
		break;
	    case 14:
		//Absolute
		    Line = String.format( "%s%02X %02X    %s $%02X%02X",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 15:
		//Absolute Indexed (With X)
		    Line = String.format( "%s%02X %02X    %s $%02X%02X,x",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Word = ( Word + cpu.X ) & 0xFFFF;
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 16:
		//Absolute Indexed (With Y)
		    Line = String.format( "%s%02X %02X    %s $%02X%02X,y",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Word += cpu.Y;
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 17:
		//Absolute long
		 Line = String.format( "%s%02X %02X %02X %s $%02X%02X%02X",
			   Line,
			   Operant[0],
			   Operant[1],
			   Operant[2],
			   Mnemonics[Opcode],
			   Operant[2],
			   Operant[1],
			   Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Line = String.format( "%-28s[$%02X:%04X]", Line, Operant[2], Word).toString();
		break;
	    case 18:
		//Absolute Indexed long
		 Line = String.format( "%s%02X %02X %02X %s $%02X%02X%02X,x",
			   Line,
			   Operant[0],
			   Operant[1],
			   Operant[2],
			   Mnemonics[Opcode],
			   Operant[2],
			   Operant[1],
			   Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Word = ( Word + cpu.X ) & 0xFFFF;
		Line = String.format( "%-28s[$%02X:%04X]", Line, Operant[2], Word).toString();
		break;
	    case 19:
		//StackRelative
		    Line = String.format( "%s%02X       %s $%02X,s",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = cpu.S;
		Word += Operant[0];
		Line = String.format( "%-28s[$00:%04X]", Line, Word).toString();
		break;
	    case 20:
		//Stack Relative Indirect Indexed
		    Line = String.format( "%s%02X       %s ($%02X,s),y",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = cpu.S;
		Word += Operant[0];
		Word = cpu.GetWord (Word);
		Word += cpu.Y;
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.DB, Word).toString();
		break;
	    case 21:
		//Absolute Indirect
		    Line = String.format( "%s%02X %02X    %s ($%02X%02X)",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.PB(), Word).toString();
		break;
	    case 22:
		//Absolute Indirect Long
		    Line = String.format( "%s%02X %02X    %s [$%02X%02X]",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Byte = cpu.GetByte (Word + 2);
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, Byte, Word).toString();
		break;
	    case 23:
		//Absolute Indexed Indirect
		    Line = String.format( "%s%02X %02X    %s ($%02X%02X,x)",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		Word = (Operant[1] << 8) | Operant[0];
		Word = ( Word + cpu.X) & 0xFFFF;
		Word = cpu.GetWord ( cpu.ShiftedPB + Word);
		Line = String.format( "%-28s[$%02X:%04X]", Line, cpu.PB(), Word).toString();
		break;
	    case 24:
		//Implied accumulator
		    Line = String.format( "%s         %s A", Line, Mnemonics[Opcode]).toString();
		break;
	    case 25:
		// MVN/MVP SRC DST
		    Line = String.format( "%s%02X %02X    %s %02X %02X", Line, Operant[0], Operant[1], Mnemonics[Opcode],
			     Operant[1], Operant[0]).toString();
		break;
	    case 26:
		// PEA
		    Line = String.format( "%s%02X %02X    %s $%02X%02X",
			     Line,
			     Operant[0],
			     Operant[1],
			     Mnemonics[Opcode],
			     Operant[1],
			     Operant[0]).toString();
		break;
	    case 27:
		// PEI Direct Indirect
		    Line = String.format( "%s%02X       %s ($%02X)",
			     Line,
			     Operant[0],
			     Mnemonics[Opcode],
			     Operant[0]).toString();
		Word = Operant[0];
		Word = ( Word + cpu.D ) & 0xFFFF;
		Word = cpu.GetWord (Word);
		Line = String.format( "%-28s[$%04X]", Line, Word).toString();
		break;
	    }

	    Line = String.format( "CPU %-40s A:%04X X:%04X Y:%04X D:%04X DB:%02X S:%04X P:%c%c%c%c%c%c%c%c%c HC:%04d VC:%03d FC:%02d %02x - 0%07d",
		    Line, cpu.A, cpu.X, cpu.Y,
		    cpu.D, cpu.DB, cpu.S,
		    cpu.CheckEmulation() ? 'E' : 'e',
			cpu.CheckNegative() ? 'N' : 'n',
			cpu.CheckOverflow() ? 'V' : 'v',
			cpu.CheckMemory() ? 'M' : 'm',
			cpu.CheckIndex() ? 'X' : 'x',
			cpu.CheckDecimal() ? 'D' : 'd',
			cpu.CheckIRQ() ? 'I' : 'i',
			cpu.CheckZero() ? 'Z' : 'z',
			cpu.CheckCarry() ? 'C' : 'c',
		    Cycles,
		    cpu.V_Counter,
			globals.ppu.FrameCount,
		    cpu.IRQActive,
		    globals.intInstCount).toString();

	    cpu.Cycles = Cycles;
	    cpu.WaitAddress = WaitAddress;
	    
	    return Line;
	}
	
	static void Trace()
	{
		System.out.println( OPrint() );
	}
}
