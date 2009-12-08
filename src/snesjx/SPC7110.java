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

class SPC7110
{
	static final int DECOMP_BUFFER_SIZE = 0x10000;
	
	private int reg4800;
	private int reg4801;
	private int reg4802;
	private int reg4803;
	private int reg4804;
	int reg4805;
	int reg4806;
	private int reg4807;
	private int reg4808;
	int reg4809;
	int reg480A;
	private int reg480B;
	private int reg480C;
	private int reg4811;
	private int reg4812;
	private int reg4813;
	private int reg4814;
	private int reg4815;
	private int reg4816;
	private int reg4817;
	private int reg4818;
	private int reg4820;
	private int reg4821;
	private int reg4822;
	private int reg4823;
	private int reg4824;
	private int reg4825;
	private int reg4826;
	private int reg4827;
	private int reg4828;
	private int reg4829;
	private int reg482A;
	private int reg482B;
	private int reg482C;
	private int reg482D;
	private int reg482E;
	private int reg482F;
	private int reg4830;
	private int reg4831;
	private int reg4832;
	private int reg4833;
	private int reg4834;
	private int reg4840;
	private int reg4841;
	private int reg4842;
	
	int AlignBy;
	private int written;
	private int offset_add;
	private int DataRomOffset;
	private int DataRomSize;
	int bank50Internal;
	ByteArray bank50 = new ByteArray(DECOMP_BUFFER_SIZE);
	
	private Memory memory;
	
	private class SPC7110RTC
	{
		private int reg[] = new int[16];
		private int index;
		private int control;
		private boolean init;
		private int last_used;
	}
	
	private SPC7110RTC S7RTC = new SPC7110RTC();
	

	//char pfold[9];				//hack variable for log naming (each game makes a different log)
	//Pack7110* decompack=NULL;   //decompression pack uses a fair chunk of RAM, so dynalloc it.
	//SPC7110Regs s7r;			//SPC7110 registers, about 33KB
	//S7RTC rtc_f9;				//FEOEZ (and Shounen Jump no SHou) RTC

	//Emulate power on state
	void Spc7110Init()
	{
		DataRomOffset=0x00100000;//handy constant!
		DataRomSize=memory.CalculatedSize-DataRomOffset;
		reg4800=0;
		reg4801=0;
		reg4802=0;
		reg4803=0;
		reg4804=0;
		reg4805=0;
		reg4806=0;
		reg4807=0;
		reg4808=0;
		reg4809=0;
		reg480A=0;
		reg480B=0;
		reg480C=0;
		reg4811=0;
		reg4812=0;
		reg4813=0;
		reg4814=0;
		reg4815=0;
		reg4816=0;
		reg4817=0;
		reg4818=0;
		reg4820=0;
		reg4821=0;
		reg4822=0;
		reg4823=0;
		reg4824=0;
		reg4825=0;
		reg4826=0;
		reg4827=0;
		reg4828=0;
		reg4829=0;
		reg482A=0;
		reg482B=0;
		reg482C=0;
		reg482D=0;
		reg482E=0;
		reg482F=0;
		reg4830=0;
		reg4831=0;
		reg4832=1;
		reg4833=2;
		reg4834=0;
		reg4840=0;
		reg4841=0;
		reg4842=0;
		written=0;
		offset_add=0;
		AlignBy=1;

		/* TODO
		(*LoadUp7110)(osd_GetPackDir());

		if (Settings.SPC7110RTC)
			Settings.TurboMode=false;

		bank50Internal=0;
		memset(bank50,0x00,DECOMP_BUFFER_SIZE);
		*/
	}


	//full cache decompression routine (memcpy) Method 1
	private void MovePackData()
	{
		/*
		//log the last entry
		Data7110* log=&(decompack.tableEnts[decompack.idx].location[decompack.last_idx]);
		if ((log.used_len+log.used_offset)<(decompack.last_offset+(int)bank50Internal))
		{
			log.used_len=bank50Internal;
			log.used_offset=decompack.last_offset;
		}

		//set up for next logging
		decompack.last_offset=(reg4805)|(reg4806<<8);

		decompack.last_idx=reg4804;

		//start decompression
		int table=(reg4803<<16)|(reg4802<<8)|reg4801;

		//the table is a offset multiplier byte and a big-endian pointer
		int j= 4*reg4804;
		j+=DataRomOffset;
		j+=table;

		//set proper offsetting.
		if (reg480B==0)
			AlignBy=0;
		else
		{
			switch (Memory.ROM[j])
			{
			case 0x03:
				AlignBy=8;
				break;
			case 0x01:
				AlignBy=2;
				break;
			case 0x02:
				AlignBy=4;
				break;
			case 0x00:
			default:
				AlignBy=1;
				break;
			}
		}
		//note that we are still setting up for the next log.
		decompack.last_offset*=AlignBy;
		decompack.last_offset%=DECOMP_BUFFER_SIZE;

		//find the table
		if (table!=decompack.last_table)
		{
			int i=0;
			while (i<MAX_TABLES&&decompack.tableEnts[i].table!=table)
				i++;
			if (i==MAX_TABLES)
			{

				FILE* fp=fopen("sp7err.out","a");

				fprintf(fp, "Table Entry %06X:%02X not found\n", table, reg4804);
				fclose(fp);
				return;
			}
			decompack.idx=i;
			decompack.last_table=table;
		}

		//copy data
		if (decompack.binfiles[decompack.idx])
		{
			memcpy(bank50,
			       &(decompack.binfiles[decompack.idx][decompack.tableEnts[decompack.idx].location[reg4804].offset]),
			       decompack.tableEnts[decompack.idx].location[reg4804].size);
		}
		*/
	}
	
	static int table_age_2;
	static int table_age_3;
	static int table_age_4;
	static int table_age_5;

	//this is similar to the last function, but it keeps the last 5 accessed files open,
	// and reads the data directly. Method 2
	private void ReadPackData()
	{
		/*
		int table = ( reg4803 << 16 ) | ( reg4802 << 8 ) | reg4801;

		if (table==0)
		{
			table_age_2=table_age_3=table_age_4=table_age_5=MAX_TABLES;
			return;
		}

		if (table_age_2==0&&table_age_3==0&&table_age_4==0&&table_age_5==0)
			table_age_2=table_age_3=table_age_4=table_age_5=MAX_TABLES;
		Data7110* log=&(decompack.tableEnts[decompack.idx].location[decompack.last_idx]);
		if ((log.used_len+log.used_offset)<(decompack.last_offset+(int)bank50Internal))
		{
			log.used_len=bank50Internal;
			log.used_offset=decompack.last_offset;
		}

		decompack.last_offset=(reg4805)|(reg4806<<8);

		decompack.last_idx=reg4804;

		int j= 4*reg4804;
		j+=DataRomOffset;
		j+=table;

		if (reg480B==0)
			AlignBy=0;
		else
		{
			switch (Memory.ROM[j])
			{
			case 0x03:
				AlignBy=8;
				break;
			case 0x01:
				AlignBy=2;
				break;
			case 0x02:
				AlignBy=4;
				break;
			case 0x00:
			default:
				AlignBy=1;
				break;
			}
		}
		decompack.last_offset*=AlignBy;
		decompack.last_offset%=DECOMP_BUFFER_SIZE;
		if (table!=decompack.last_table)
		{
			int i=0;
			while (i<MAX_TABLES&&decompack.tableEnts[i].table!=table)
				i++;
			if (i==MAX_TABLES)
			{
				FILE* fp=fopen("sp7err.out","a");

				fprintf(fp, "Table Entry %06X:%02X not found\n", table, reg4804);
				fclose(fp);
				return;
			}
			if (i!= table_age_2 && i!= table_age_3 && i!= table_age_4 && i!= table_age_5)
			{
				if (table_age_5!=MAX_TABLES&&decompack.binfiles[table_age_5])
				{
					fclose((FILE*)(decompack.binfiles[table_age_5]));
					(decompack.binfiles[table_age_5])=NULL;
				}
				table_age_5=table_age_4;
				table_age_4=table_age_3;
				table_age_3=table_age_2;
				table_age_2=decompack.idx;

				char name[PATH_MAX];
				//open file
				char drive [_MAX_DRIVE + 1];
				char dir [_MAX_DIR + 1];
				char fname [_MAX_FNAME + 1];
				char ext [_MAX_EXT + 1];
				const char* patchDir = GetDirectory (PATCH_DIR);
				if (patchDir && strlen (patchDir))
				{
					//splitpath (Memory.ROMFilename, drive, dir, fname, ext);
					strcpy (name, patchDir);
					strcat (name, "/");
				}
				else
				{
					splitpath (Memory.ROMFilename, drive, dir, fname, ext);
					strcpy(name, drive);
					//strcat(filename, "\\");
					strcat(name, dir);
				}
				strcat(name, pfold);
				char bfname[11];
				sprintf(bfname, "%06X.bin", table);
				strcat(name, "/");
				strcat(name, bfname);

				decompack.binfiles[i]=(uint8*)fopen(name, "rb");
			}
			else
			{
				//fix tables in this case
				if (table_age_5==i)
				{
					table_age_5=table_age_4;
					table_age_4=table_age_3;
					table_age_3=table_age_2;
					table_age_2=decompack.idx;
				}
				if (table_age_4==i)
				{
					table_age_4=table_age_3;
					table_age_3=table_age_2;
					table_age_2=decompack.idx;
				}
				if (table_age_3==i)
				{
					table_age_3=table_age_2;
					table_age_2=decompack.idx;
				}
				if (table_age_2==i)
				{
					table_age_2=decompack.idx;
				}
			}
			decompack.idx=i;
			decompack.last_table=table;
		}
		//do read here.
		if (decompack.binfiles[decompack.idx])
		{
			fseek((FILE*)(decompack.binfiles[decompack.idx]), decompack.tableEnts[decompack.idx].location[reg4804].offset, 0);
			fread(bank50,1, (decompack.tableEnts[decompack.idx].location[reg4804].size), (FILE*)(decompack.binfiles[decompack.idx]));
		}
		*/
	}

	//Cache Method 3: some entries are cached, others are file handles.
	//use is_file to distinguish.
	private void GetPackData()
	{
		/*
		Data7110* log=&(decompack.tableEnts[decompack.idx].location[decompack.last_idx]);
		if ((log.used_len+log.used_offset)<(decompack.last_offset+(int)bank50Internal))
		{
			log.used_len=bank50Internal;
			log.used_offset=decompack.last_offset;
		}

		decompack.last_offset=(reg4805)|(reg4806<<8);

		decompack.last_idx=reg4804;
		int table=(reg4803<<16)|(reg4802<<8)|reg4801;

		int j= 4*reg4804;
		j+=DataRomOffset;
		j+=table;

		if (reg480B==0)
			AlignBy=0;
		else
		{
			switch (Memory.ROM[j])
			{
			case 0x03:
				AlignBy=8;
				break;
			case 0x01:
				AlignBy=2;
				break;
			case 0x02:
				AlignBy=4;
				break;
			case 0x00:
			default:
				AlignBy=1;
				break;
			}
		}
		decompack.last_offset*=AlignBy;
		decompack.last_offset%=DECOMP_BUFFER_SIZE;
		if (table!=decompack.last_table)
		{
			int i=0;
			while (i<MAX_TABLES&&decompack.tableEnts[i].table!=table)
				i++;
			if (i==MAX_TABLES)
			{

				FILE* fp=fopen("sp7err.out","a");
				fprintf(fp, "Table Entry %06X:%02X not found\n", table, reg4804);
				fclose(fp);
				return;
			}
			decompack.idx=i;
			decompack.last_table=table;
		}
		if (decompack.binfiles[decompack.idx])
		{
			if (decompack.tableEnts[decompack.idx].is_file)
			{
				fseek((FILE*)decompack.binfiles[decompack.idx], decompack.tableEnts[decompack.idx].location[reg4804].offset, 0);
				fread(bank50,1, (decompack.tableEnts[decompack.idx].location[reg4804].size), (FILE*)(decompack.binfiles[decompack.idx]));
			}
			else
			{
				memcpy(bank50,
				       &(decompack.binfiles[decompack.idx][decompack.tableEnts[decompack.idx].location[reg4804].offset]),
				       decompack.tableEnts[decompack.idx].location[reg4804].size);
			}
		}
		*/
	}


	//reads SPC7110 and RTC registers.
	int GetSPC7110(int Address)
	{
		/*
		switch (Address)
		{
			//decompressed data read port. decrements 4809-A (with wrap)
			//4805-6 is the offset into the bank
			//AlignBy is set (afaik) at decompression time, and is the offset multiplier
			//bank50internal is an internal pointer to the actual byte to read.
			//so you read from offset*multiplier + bank50internal
			//the offset registers cannot be incremented due to the offset multiplier.
		case 0x4800:
		{
			int count=reg4809|(reg480A<<8);
			int i, j;
			j=(reg4805|(reg4806<<8));
			j*=AlignBy;
			i=j;
			if (count >0)
				count--;
			else count = 0xFFFF;
			reg4809=0x00ff&count;
			reg480A=(0xff00&count)>>8;
			i+=bank50Internal;
			i%=DECOMP_BUFFER_SIZE;
			reg4800=bank50[i];

			bank50Internal++;
			bank50Internal%=DECOMP_BUFFER_SIZE;

		}
		return reg4800;
		//table register low
		case 0x4801:
			return reg4801;
			//table register middle
		case 0x4802:
			return reg4802;
			//table register high
		case 0x4803:
			return reg4803;
			//index of pointer in table (each entry is 4 bytes)
		case 0x4804:
			return reg4804;
			//offset register low
		case 0x4805:
			return reg4805;
			//offset register high
		case 0x4806:
			return reg4806;
			//DMA channel (not that I see this usually set,
			//regardless of what channel DMA is on)
		case 0x4807:
			return reg4807;
			//C r/w option, unknown, defval:00 is what Dark Force says
			//afaict, Sne doesn't use this at all.
		case 0x4808:
			return reg4808;
			//C-Length low
			//counts down the number of bytes left to read from the decompression buffer.
			//this is set by the ROM, and wraps on bounds.
		case 0x4809:
			return reg4809;
			//C Length high
		case 0x480A:
			return reg480A;
			//Offset enable.
			//if this is zero, 4805-6 are useless. Emulated by setting AlignBy to 0
		case 0x480B:
			return reg480B;
			//decompression finished: just emulated by switching each read.
		case 0x480C:
			reg480C^=0x80;
			return reg480C^0x80;

			//Data access port
			//reads from the data ROM (anywhere over the first 8 mbits
			//behavior is complex, will document later,
			//possibly missing cases, because of the number of switches in play
		case 0x4810:
			if (written==0)
				return 0;
			if ((written&0x07)==0x07)
			{
				int i=(reg4813<<16)|(reg4812<<8)|reg4811;
				i%=DataRomSize;
				if (reg4818&0x02)
				{
					if (reg4818&0x08)
					{
						signed short r4814;
						r4814=(reg4815<<8)|reg4814;
						i+=r4814;
						r4814++;
						reg4815=(uint8)(r4814>>8);
						reg4814=(uint8)(r4814&0x00FF);
					}
					else
					{
						int r4814;
						r4814=(reg4815<<8)|reg4814;
						i+=r4814;
						if (r4814!=0xFFFF)
							r4814++;
						else r4814=0;
						reg4815=(uint8)(r4814>>8);
						reg4814=(uint8)(r4814&0x00FF);

					}
				}
				i+=DataRomOffset;
				uint8 tmp=Memory.ROM[i];
				i=(reg4813<<16)|(reg4812<<8)|reg4811;

				if (reg4818&0x02)
				{
				}
				else if (reg4818&0x01)
				{
					if (reg4818&0x04)
					{
						signed short inc;
						inc=(reg4817<<8)|reg4816;

						if (!(reg4818&0x10))
							i+=inc;
						else
						{
							if (reg4818&0x08)
							{
								signed short r4814;
								r4814=(reg4815<<8)|reg4814;
								r4814+=inc;
								reg4815=(r4814&0xFF00)>>8;
								reg4814=r4814&0xFF;
							}
							else
							{
								int r4814;
								r4814=(reg4815<<8)|reg4814;
								r4814+=inc;
								reg4815=(r4814&0xFF00)>>8;
								reg4814=r4814&0xFF;

							}
						}
						//is signed
					}
					else
					{
						uint16 inc;
						inc=(reg4817<<8)|reg4816;
						if (!(reg4818&0x10))
							i+=inc;
						else
						{
							if (reg4818&0x08)
							{
								signed short r4814;
								r4814=(reg4815<<8)|reg4814;
								r4814+=inc;
								reg4815=(r4814&0xFF00)>>8;
								reg4814=r4814&0xFF;
							}
							else
							{
								int r4814;
								r4814=(reg4815<<8)|reg4814;
								r4814+=inc;
								reg4815=(r4814&0xFF00)>>8;
								reg4814=r4814&0xFF;

							}
						}
					}
				}
				else
				{
					if (!(reg4818&0x10))
						i+=1;
					else
					{
						if (reg4818&0x08)
						{
							signed short r4814;
							r4814=(reg4815<<8)|reg4814;
							r4814+=1;
							reg4815=(r4814&0xFF00)>>8;
							reg4814=r4814&0xFF;
						}
						else
						{
							int r4814;
							r4814=(reg4815<<8)|reg4814;
							r4814+=1;
							reg4815=(r4814&0xFF00)>>8;
							reg4814=r4814&0xFF;

						}
					}
				}

				i%=DataRomSize;
				reg4811=i&0x00FF;
				reg4812=(i&0x00FF00)>>8;
				reg4813=((i&0xFF0000)>>16);
				return tmp;
			}
			else return 0;
			//direct read address low
		case 0x4811:
			return reg4811;
			//direct read address middle
		case 0x4812:
			return reg4812;
			//direct read access high
		case 0x4813:
			return reg4813;
			//read adjust low
		case 0x4814:
			return reg4814;
			//read adjust high
		case 0x4815:
			return reg4815;
			//read increment low
		case 0x4816:
			return reg4816;
			//read increment high
		case 0x4817:
			return reg4817;
			//Data ROM command mode
			//essentially, this controls the insane code of $4810 and $481A
		case 0x4818:
			return reg4818;
			//read after adjust port
			//what this does, besides more nasty stuff like 4810,
			//I don't know. Just assume it is a different implementation of $4810,
			//if it helps your sanity
		case 0x481A:
			if (written==0x1F)
			{
				int i=((reg4813<<16)|(reg4812<<8)|reg4811);
				if (reg4818&0x08)
				{
					short adj;
					adj=((short)(reg4815<<8))|reg4814;
					i+=adj;
				}
				else
				{
					uint16 adj;
					adj=(reg4815<<8)|reg4814;
					i+=adj;
				}

				i%=DataRomSize;
				i+=DataRomOffset;
				uint8 tmp=Memory.ROM[i];
				i=((reg4813<<16)|(reg4812<<8)|reg4811);
				if (0x60==(reg4818&0x60))
				{
					i=((reg4813<<16)|(reg4812<<8)|reg4811);

					if (!(reg4818&0x10))
					{
						if (reg4818&0x08)
						{
							short adj;
							adj=((short)(reg4815<<8))|reg4814;
							i+=adj;
						}
						else
						{
							uint16 adj;
							adj=(reg4815<<8)|reg4814;
							i+=adj;
						}
						i%=DataRomSize;
						reg4811=i&0x00FF;
						reg4812=(i&0x00FF00)>>8;
						reg4813=((i&0xFF0000)>>16);
					}
					else
					{
						if (reg4818&0x08)
						{
							short adj;
							adj=((short)(reg4815<<8))|reg4814;
							adj+=adj;
							reg4815=(adj&0xFF00)>>8;
							reg4814=adj&0xFF;
						}
						else
						{
							uint16 adj;
							adj=(reg4815<<8)|reg4814;
							adj+=adj;
							reg4815=(adj&0xFF00)>>8;
							reg4814=adj&0xFF;
						}
					}
				}

				return tmp;
			}
			else return 0;

			//multiplicand low or dividend lowest
		case 0x4820:
			return reg4820;
			//multiplicand high or divdend lower
		case 0x4821:
			return reg4821;
			//dividend higher
		case 0x4822:
			return reg4822;
			//dividend highest
		case 0x4823:
			return reg4823;
			//multiplier low
		case 0x4824:
			return reg4824;
			//multiplier high
		case 0x4825:
			return reg4825;
			//divisor low
		case 0x4826:
			return reg4826;
			//divisor high
		case 0x4827:
			return reg4827;

			//result lowest
		case 0x4828:
			return reg4828;
			//result lower
		case 0x4829:
			return reg4829;
			//result higher
		case 0x482A:
			return reg482A;
			//result highest
		case 0x482B:
			return reg482B;
			//remainder (division) low
		case 0x482C:
			return reg482C;
			//remainder (division) high
		case 0x482D:
			return reg482D;
			//signed/unsigned
		case 0x482E:
			return reg482E;
			//finished flag, emulated as an on-read toggle.
		case 0x482F:
			if (reg482F)
			{
				reg482F=0;
				return 0x80;
			}
			return 0;
			break;

			//SRAM toggle
		case 0x4830:
			return reg4830;
			//DX bank mapping
		case 0x4831:
			return reg4831;
			//EX bank mapping
		case 0x4832:
			return reg4832;
			//FX bank mapping
		case 0x4833:
			return reg4833;
			//SRAM mapping? We have no clue!
		case 0x4834:
			return reg4834;
//RTC enable
		case 0x4840:
			if (!Settings.SPC7110RTC)
				return Address>>8;
			return reg4840;
//command/index/value of RTC (essentially, zero unless we're in read mode
		case 0x4841:
			if (!Settings.SPC7110RTC)
				return Address>>8;
			if (rtc_f9.init)
			{
				UpdateRTC();
				uint8 tmp=rtc_f9.reg[rtc_f9.index];
				rtc_f9.index++;
				rtc_f9.index%=0x10;
				return tmp;
			}
			else return 0;
			//RTC done flag
		case 0x4842:
			if (!Settings.SPC7110RTC)
				return Address>>8;
			reg4842^=0x80;
			return reg4842^0x80;
		default:

			return 0x00;
		}
		*/
		
		return 0x00;
	}

	void SetSPC7110(int data, int Address)
	{
		/*
		switch (Address)
		{
		//Writes to $4800 are undefined.

			//table low, middle, and high.
		case 0x4801:
			reg4801=data;
			break;
		case 0x4802:
			reg4802=data;
			break;
		case 0x4803:
			reg4803=data;
			break;

			//table index (4 byte entries, bigendian with a multiplier byte)
		case 0x4804:
			reg4804=data;
			break;

			//offset low
		case 0x4805:
			reg4805=data;
			break;

			//offset high, starts decompression
		case 0x4806:
			reg4806=data;
			(*Copy7110)();
			bank50Internal=0;
			reg480C&=0x7F;
			break;

			//DMA channel register (Is it used??)
		case 0x4807:
			reg4807=data;
			break;

			//C r/w? I have no idea. If you get weird values written here before a bug,
			//The Dumper should probably be contacted about running a test.
		case 0x4808:
			reg4808=data;
			break;

			//C-Length low
		case 0x4809:
			reg4809=data;
			break;
			//C-Length high
		case 0x480A:
			reg480A=data;
			break;

			//Offset enable
		case 0x480B:
		{
			reg480B=data;
			int table=(reg4803<<16)|(reg4802<<8)|reg4801;

			int j= 4*reg4804;
			j+=DataRomOffset;
			j+=table;

			if (reg480B==0)
				AlignBy=0;
			else
			{
				switch (Memory.ROM[j])
				{
				case 0x03:
					AlignBy=8;
					break;
				case 0x01:
					AlignBy=2;
					break;
				case 0x02:
					AlignBy=4;
					break;
				case 0x00:
				default:
					AlignBy=1;
					break;
				}
			}
//				decomp_set=true;
		}
		break;
	//$4810 is probably read only.

		//Data port address low
		case 0x4811:
			reg4811=data;
			written|=0x01;
			break;

			//data port address middle
		case 0x4812:
			reg4812=data;
			written|=0x02;
			break;

			//data port address high
		case 0x4813:
			reg4813=data;
			written|=0x04;
			break;

			//data port adjust low (has a funky immediate increment mode)
		case 0x4814:
			reg4814=data;
			if (reg4818&0x02)
			{
				if ((reg4818&0x20)&&!(reg4818&0x40))
				{
					offset_add|=0x01;
					if (offset_add==3)
					{
						if (reg4818&0x10)
						{
						}
						else
						{
							int i=(reg4813<<16)|(reg4812<<8)|reg4811;
							if (reg4818&0x08)
							{
								i+=(signed char)reg4814;
							}
							else
							{
								i+=reg4814;
							}
							i%=DataRomSize;
							reg4811=i&0x00FF;
							reg4812=(i&0x00FF00)>>8;
							reg4813=((i&0xFF0000)>>16);
						}
					}
				}
				else if ((reg4818&0x40)&&!(reg4818&0x20))
				{
					offset_add|=0x01;
					if (offset_add==3)
					{
						if (reg4818&0x10)
						{
						}
						else
						{
							int i=(reg4813<<16)|(reg4812<<8)|reg4811;
							if (reg4818&0x08)
							{
								short adj;
								adj=((short)(reg4815<<8))|reg4814;
								i+=adj;
							}
							else
							{
								uint16 adj;
								adj=(reg4815<<8)|reg4814;
								i+=adj;
							}
							i%=DataRomSize;
							reg4811=i&0x00FF;
							reg4812=(i&0x00FF00)>>8;
							reg4813=((i&0xFF0000)>>16);
						}
					}

				}
			}

			written|=0x08;
			break;

			//data port adjust high (has a funky immediate increment mode)
		case 0x4815:
			reg4815=data;
			if (reg4818&0x02)
			{
				if (reg4818&0x20&&!(reg4818&0x40))
				{
					offset_add|=0x02;
					if (offset_add==3)
					{
						if (reg4818&0x10)
						{
						}
						else
						{
							int i=(reg4813<<16)|(reg4812<<8)|reg4811;

							if (reg4818&0x08)
							{
								i+=(signed char)reg4814;
							}
							else
							{
								i+=reg4814;
							}
							i%=DataRomSize;
							reg4811=i&0x00FF;
							reg4812=(i&0x00FF00)>>8;
							reg4813=((i&0xFF0000)>>16);
						}
					}
				}
				else if (reg4818&0x40&&!(reg4818&0x20))
				{
					offset_add|=0x02;
					if (offset_add==3)
					{
						if (reg4818&0x10)
						{
						}
						else
						{
							int i=(reg4813<<16)|(reg4812<<8)|reg4811;
							if (reg4818&0x08)
							{
								short adj;
								adj=((short)(reg4815<<8))|reg4814;
								i+=adj;
							}
							else
							{
								uint16 adj;
								adj=(reg4815<<8)|reg4814;
								i+=adj;
							}
							i%=DataRomSize;
							reg4811=i&0x00FF;
							reg4812=(i&0x00FF00)>>8;
							reg4813=((i&0xFF0000)>>16);
						}
					}
				}
			}
			written|=0x10;
			break;
			//data port increment low
		case 0x4816:
			reg4816=data;
			break;
			//data port increment high
		case 0x4817:
			reg4817=data;
			break;

			//data port mode switches
			//note that it starts inactive.
		case 0x4818:
			if ((written&0x18)!=0x18)
				break;
			offset_add=0;
			reg4818=data;
			break;

			//multiplicand low or dividend lowest
		case 0x4820:
			reg4820=data;
			break;
			//multiplicand high or dividend lower
		case 0x4821:
			reg4821=data;
			break;
			//dividend higher
		case 0x4822:
			reg4822=data;
			break;
			//dividend highest
		case 0x4823:
			reg4823=data;
			break;
			//multiplier low
		case 0x4824:
			reg4824=data;
			break;
			//multiplier high (triggers operation)
		case 0x4825:
			reg4825=data;
			if (reg482E&0x01)
			{
				int mul;
				short m1=(short)((reg4824)|(reg4825<<8));
				short m2=(short)((reg4820)|(reg4821<<8));

				mul=m1*m2;
				reg4828=(uint8)(mul&0x000000FF);
				reg4829=(uint8)((mul&0x0000FF00)>>8);
				reg482A=(uint8)((mul&0x00FF0000)>>16);
				reg482B=(uint8)((mul&0xFF000000)>>24);
			}
			else
			{
				int mul;
				uint16 m1=(uint16)((reg4824)|(reg4825<<8));
				uint16 m2=(uint16)((reg4820)|(reg4821<<8));

				mul=m1*m2;
				reg4828=(uint8)(mul&0x000000FF);
				reg4829=(uint8)((mul&0x0000FF00)>>8);
				reg482A=(uint8)((mul&0x00FF0000)>>16);
				reg482B=(uint8)((mul&0xFF000000)>>24);
			}
			reg482F=0x80;
			break;
			//divisor low
		case 0x4826:
			reg4826=data;
			break;
			//divisor high (triggers operation)
		case 0x4827:
			reg4827=data;
			if (reg482E&0x01)
			{
				int quotient;
				short remainder;
				int dividend=(int)(reg4820|(reg4821<<8)|(reg4822<<16)|(reg4823<<24));
				short divisor=(short)(reg4826|(reg4827<<8));
				if (divisor != 0)
				{
					quotient=(int)(dividend/divisor);
					remainder=(short)(dividend%divisor);
				}
				else
				{
					quotient=0;
					remainder=dividend&0x0000FFFF;
				}
				reg4828=(uint8)(quotient&0x000000FF);
				reg4829=(uint8)((quotient&0x0000FF00)>>8);
				reg482A=(uint8)((quotient&0x00FF0000)>>16);
				reg482B=(uint8)((quotient&0xFF000000)>>24);
				reg482C=(uint8)remainder&0x00FF;
				reg482D=(uint8)((remainder&0xFF00)>>8);
			}
			else
			{
				int quotient;
				uint16 remainder;
				int dividend=(int)(reg4820|(reg4821<<8)|(reg4822<<16)|(reg4823<<24));
				uint16 divisor=(uint16)(reg4826|(reg4827<<8));
				if (divisor != 0)
				{
					quotient=(int)(dividend/divisor);
					remainder=(uint16)(dividend%divisor);
				}
				else
				{
					quotient=0;
					remainder=dividend&0x0000FFFF;
				}
				reg4828=(uint8)(quotient&0x000000FF);
				reg4829=(uint8)((quotient&0x0000FF00)>>8);
				reg482A=(uint8)((quotient&0x00FF0000)>>16);
				reg482B=(uint8)((quotient&0xFF000000)>>24);
				reg482C=(uint8)remainder&0x00FF;
				reg482D=(uint8)((remainder&0xFF00)>>8);
			}
			reg482F=0x80;
			break;
			//result registers are possibly read-only

			//reset: writes here nuke the whole math unit
			//Zero indicates unsigned math, resets with non-zero values turn on signed math
		case 0x482E:
			reg4820=reg4821=reg4822=reg4823=reg4824=reg4825=reg4826=reg4827=reg4828=reg4829=reg482A=reg482B=reg482C=reg482D=0;
			reg482E=data;
			break;

			//math status register possibly read only

			//SRAM toggle
		case 0x4830:
			SetSPC7110SRAMMap(data);
			reg4830=data;
			break;
			//Bank DX mapping
		case 0x4831:
			reg4831=data;
			break;
			//Bank EX mapping
		case 0x4832:
			reg4832=data;
			break;
			//Bank FX mapping
		case 0x4833:
			reg4833=data;
			break;
			//S-RAM mapping? who knows?
		case 0x4834:
			reg4834=data;
			break;
			//RTC Toggle
		case 0x4840:
			if (0==data)
			{
				UpdateRTC();
				//	rtc_f9.init=false;
				//	rtc_f9.index=-1;
			}
			if (data&0x01)
			{
				reg4842=0x80;
				//rtc_f9.last_used=time(NULL);//????
				rtc_f9.init=false;
				rtc_f9.index=-1;
			}
			reg4840=data;
			break;
			//RTC init/command/index register
		case 0x4841:
			if (rtc_f9.init)
			{
				if (-1==rtc_f9.index)
				{
					rtc_f9.index=data&0x0F;
					break;
				}
				if (rtc_f9.control==0x0C)
				{
					rtc_f9.index=data&0x0F;
					reg4842=0x80;
					rtc_f9.last_used=time(NULL);
				}
				else
				{

					if (0x0D==rtc_f9.index)
					{
						if (data&0x08)
						{
							if (rtc_f9.reg[1]<3)
							{
								UpdateRTC();
								rtc_f9.reg[0]=0;
								rtc_f9.reg[1]=0;
								rtc_f9.last_used=time(NULL);
							}
							else
							{
								UpdateRTC();
								rtc_f9.reg[0]=0;
								rtc_f9.reg[1]=0;
								rtc_f9.last_used=time(NULL)-60;
								UpdateRTC();
								rtc_f9.last_used=time(NULL);
							}
							data&=0x07;
						}
						if (rtc_f9.reg[0x0D]&0x01)
						{
							if (!(data%2))
							{
								rtc_f9.reg[rtc_f9.index&0x0F]=data;
								rtc_f9.last_used=time(NULL)-1;
								UpdateRTC();
								rtc_f9.last_used=time(NULL);
							}
						}
					}
					if (0x0F==rtc_f9.index)
					{
						if (data&0x01&&!(rtc_f9.reg[0x0F]&0x01))
						{
							UpdateRTC();
							rtc_f9.reg[0]=0;
							rtc_f9.reg[1]=0;
							rtc_f9.last_used=time(NULL);
						}
						if (data&0x02&&!(rtc_f9.reg[0x0F]&0x02))
						{
							UpdateRTC();
							rtc_f9.last_used=time(NULL);
						}
					}
					rtc_f9.reg[rtc_f9.index&0x0F]=data;
					reg4842=0x80;
					rtc_f9.index=(rtc_f9.index+1)%0x10;
				}
			}
			else
			{
				if (data==0x03||data==0x0C)
				{
					rtc_f9.init=true;
					rtc_f9.control=data;
					rtc_f9.index=-1;
				}
			}
			break;
			//writes to RTC status register aren't expected to be meaningful
		default:
			Address-=0x4800;
			break;
			//16 BIT MULTIPLIER: ($FF00) high byte, defval:00
		}
		*/
	}

	//emulate the SPC7110's ability to remap banks Dx, Ex, and Fx.
	int GetSPC7110Byte(int Address)
	{
		int i;
		switch ((Address&0x00F00000)>>16)
		{
		case 0xD0:
			i=reg4831*0x00100000;
			break;
		case 0xE0:
			i=reg4832*0x00100000;
			break;
		case 0xF0:
			i=reg4833*0x00100000;
			break;
		default:
			i=0;
		}
		i+=Address&0x000FFFFF;
		i+=DataRomOffset;
		return memory.ROM.get8Bit(i);
	}

	// Return the number of days in a specific month for a certain year                           */
	// copied directly for RTC functionality, separated in case of incompatibilities			  */
	int	RTCDaysInMonth( int month, int year )
	{
		int		mdays;

		switch ( month )
		{
		case 2:
			if ( ( year % 4 == 0 ) )    // DKJM2 only uses 199x - 22xx
				mdays = 29;
			else
				mdays = 28;
			break;

		case 4:
		case 6:
		case 9:
		case 11:
			mdays = 30;
			break;

		default:	// months 1,3,5,7,8,10,12
			mdays = 31;
			break;
		}

		return mdays;
	}


	private int DAYTICKS = (60*60*24);
	private int HOURTICKS = (60*60);
	private int MINUTETICKS = 60;

	// Advance the RTC time
	private void UpdateRTC()
	{
		/*
		time_t	cur_systime;
		long    time_diff;

		// Keep track of game time by computing the number of seconds that pass on the system
		// clock and adding the same number of seconds to the RTC clock structure.

		if (rtc_f9.init && 0==(rtc_f9.reg[0x0D]&0x01) && 0==(rtc_f9.reg[0x0F]&0x03))
		{
			cur_systime = time (NULL);

			// This method assumes one time_t clock tick is one second
			//        which should work on PCs and GNU systems.
			//        If your tick interval is different adjust the
			//        DAYTICK, HOURTICK, and MINUTETICK defines

			time_diff = (long) (cur_systime - rtc_f9.last_used);
			rtc_f9.last_used = cur_systime;

			if ( time_diff > 0 )
			{
				int		seconds;
				int		minutes;
				int		hours;
				int		days;
				int		month;
				int		year;
				int		temp_days;

				int		year_hundreds;
				int		year_tens;
				int		year_ones;


				if ( time_diff > DAYTICKS )
				{
					days = time_diff / DAYTICKS;
					time_diff = time_diff - days * DAYTICKS;
				}
				else
				{
					days = 0;
				}

				if ( time_diff > HOURTICKS )
				{
					hours = time_diff / HOURTICKS;
					time_diff = time_diff - hours * HOURTICKS;
				}
				else
				{
					hours = 0;
				}

				if ( time_diff > MINUTETICKS )
				{
					minutes = time_diff / MINUTETICKS;
					time_diff = time_diff - minutes * MINUTETICKS;
				}
				else
				{
					minutes = 0;
				}

				if ( time_diff > 0 )
				{
					seconds = time_diff;
				}
				else
				{
					seconds = 0;
				}


				seconds += (rtc_f9.reg[1]*10 + rtc_f9.reg[0]);
				if ( seconds >= 60 )
				{
					seconds -= 60;
					minutes += 1;
				}

				minutes += (rtc_f9.reg[3]*10 + rtc_f9.reg[2]);
				if ( minutes >= 60 )
				{
					minutes -= 60;
					hours += 1;
				}

				hours += (rtc_f9.reg[5]*10 + rtc_f9.reg[4]);
				if ( hours >= 24 )
				{
					hours -= 24;
					days += 1;
				}

				year =  rtc_f9.reg[11]*10 + rtc_f9.reg[10];
				year += ( 1900 );
				month = rtc_f9.reg[8]+10*rtc_f9.reg[9];
				rtc_f9.reg[12]+=days;
				days += (rtc_f9.reg[7]*10 + rtc_f9.reg[6]);
				if ( days > 0 )
				{
					while ( days > (temp_days = RTCDaysInMonth( month, year )) )
					{
						days -= temp_days;
						month += 1;
						if ( month > 12 )
						{
							year += 1;
							month = 1;
						}
					}
				}

				year_tens = year % 100;
				year_ones = year_tens % 10;
				year_tens /= 10;
				year_hundreds = (year - 1000) / 100;

				rtc_f9.reg[0] = seconds % 10;
				rtc_f9.reg[1] = seconds / 10;
				rtc_f9.reg[2] = minutes % 10;
				rtc_f9.reg[3] = minutes / 10;
				rtc_f9.reg[4] = hours % 10;
				rtc_f9.reg[5] = hours / 10;
				rtc_f9.reg[6] = days % 10;
				rtc_f9.reg[7] = days / 10;
				rtc_f9.reg[8] = month%10;
				rtc_f9.reg[9] = month /10;
				rtc_f9.reg[10] = year_ones;
				rtc_f9.reg[11] = year_tens;
				rtc_f9.reg[12] %= 7;
				return;
			}
		}
		*/
	}

	//allows DMA from the ROM (is this even possible on the SPC7110?
	ByteArrayOffset Get7110BasePtr(int Address)
	{
		int i;
		switch ((Address&0x00F00000)>>16)
		{
		case 0xD0:
			i=reg4831*0x100000;
			break;
		case 0xE0:
			i=reg4832*0x100000;
			break;
		case 0xF0:
			i=reg4833*0x100000;
			break;
		default:
			i=0;
		}
		i+=Address&0x000F0000;
		
		return memory.ROM.getOffsetBuffer(i);
	}


	//loads the index into memory.
	//index.bin is little-endian
	//format index (1)-table(3)-file offset(4)-length(4)
	private boolean Load7110Index(String filename)
	{
		/*
		FILE* fp;
		uint8 buffer[12];
		int table=0;
		uint8 index=0;
		int offset=0;
		int size=0;
		int i=0;
		fp=fopen(filename, "rb");
		if (NULL==fp)
			return false;
		do
		{
			i=0;
			fread(buffer, 1, 12,fp);
			table=(buffer[3]<<16)|(buffer[2]<<8)|buffer[1];
			index=buffer[0];
			offset=(buffer[7]<<24)|(buffer[6]<<16)|(buffer[5]<<8)|buffer[4];
			size=(buffer[11]<<24)|(buffer[10]<<16)|(buffer[9]<<8)|buffer[8];
			while (i<MAX_TABLES&&decompack.tableEnts[i].table!=table&&decompack.tableEnts[i].table!=0)
				i++;
			if (i==MAX_TABLES)
				return false;
			//added
			decompack.tableEnts[i].table=table;
			//-----
			decompack.tableEnts[i].location[index].offset=offset;
			decompack.tableEnts[i].location[index].size=size;
			decompack.tableEnts[i].location[index].used_len=0;
			decompack.tableEnts[i].location[index].used_offset=0;

		}
		while (!feof(fp));
		fclose(fp);
		*/
		return true;
	}


	//Cache 1 load function
	private void SPC7110Load(String dirname)
	{
		/*
		char temp_path[PATH_MAX];
		int i=0;

		decompack=new Pack7110;

		ZeroMemory(decompack, sizeof(Pack7110));

		// D:\\ is always app.path in Xbox
		Load7110Index("d:\\index.bin");

		for (i=0;i<MAX_TABLES;i++)
		{
			if (decompack.tableEnts[i].table!=0)
			{
				char binname[PATH_MAX];

				sprintf(binname,"%s%06X.bin",filename,decompack.tableEnts[i].table);

				struct stat buf;
				if (-1!=stat(binname, &buf))
					decompack.binfiles[i]=new uint8[buf.st_size];
				FILE* fp=fopen(binname, "rb");
				if (fp)
				{
					fread(decompack.binfiles[i],buf.st_size,1,fp);
					fclose(fp);
				}
			}
		}

		Copy7110=&MovePackData;
		CleanUp7110=&Del7110Gfx;
		
		*/
	}

	//Cache 2 load function
	private void SPC7110Open(String dirname)
	{
		/*
		char temp_path[PATH_MAX];
		int i=0;

		decompack=new Pack7110;

		getcwd(temp_path,PATH_MAX);


		ZeroMemory(decompack, sizeof(Pack7110));

		if (-1==chdir(dirname))
		{
			Message(0,0,"Graphics Pack not found!");
		}

		Load7110Index("index.bin");

		for (i=0; i<MAX_TABLES; i++)
			decompack.binfiles[i]=NULL;

		ReadPackData();

		chdir(temp_path);

		Copy7110=&ReadPackData;
		CleanUp7110=&Close7110Gfx;

		EnableMenuItem(GUI.hMenu, IDM_LOG_7110, MF_ENABLED);
		*/
	}

	//Cache 3's load function
	private void SPC7110Grab(String dirname)
	{
		/*
		char temp_path[PATH_MAX];
		int i=0;

		decompack=new Pack7110;

		getcwd(temp_path,PATH_MAX);

		int buffer_size=1024*1024*cacheMegs;//*some setting

		ZeroMemory(decompack, sizeof(Pack7110));

		if (-1==chdir(dirname))
		{
			Message(0,0,"Graphics Pack not found!");
		}

		Load7110Index("index.bin");

		for (i=0;i<MAX_TABLES;i++)
		{
			if (decompack.tableEnts[i].table!=0)
			{
				char binname[PATH_MAX];
				struct stat buf;
	//add load/no load calculations here
				if (-1!=stat(binname, &buf))
				{
					if (buf.st_size<buffer_size)
						decompack.binfiles[i]=new uint8[buf.st_size];
					FILE* fp=fopen(binname, "rb");
					//use them here
					if (fp)
					{
						if (buf.st_size<buffer_size)
						{
							fread(decompack.binfiles[i],buf.st_size,1,fp);
							fclose(fp);
							buffer_size-=buf.st_size;
							decompack.tableEnts[i].is_file=false;
						}
						else
						{
							decompack.binfiles[i]=(uint8*)fp;
							decompack.tableEnts[i].is_file=true;
						}
					}
				}
			}
		}


		chdir(temp_path);

		Copy7110=&GetPackData;
		CleanUp7110=&Drop7110Gfx;
		*/

	}

	//Cache 1 clean up function
	private void Del7110Gfx()
	{
		/*
		int i;
		if (Settings.SPC7110)
		{
			Do7110Logging();
		}
		for (i=0;i<MAX_TABLES;i++)
		{
			if (decompack.binfiles[i]!=NULL)
			{
				//TODO: delete []decompack.binfiles[i];
				decompack.binfiles[i]=NULL;
			}
		}
		Settings.SPC7110=false;
		Settings.SPC7110RTC=false;
		if (NULL!=decompack)
			delete decompack;
		decompack=NULL;
		CleanUp7110=NULL;
		Copy7110=NULL;
		*/
	}

	//Cache2 cleanup function
	private void Close7110Gfx()
	{
		/*
		int i;
		if (Settings.SPC7110)
		{
			Do7110Logging();
		}
		for (i=0;i<MAX_TABLES;i++)
		{
			if (decompack.binfiles[i]!=NULL)
			{
				fclose((FILE*)decompack.binfiles[i]);
				decompack.binfiles[i]=NULL;
			}
		}
		Settings.SPC7110=false;
		Settings.SPC7110RTC=false;
		if (NULL!=decompack)
			delete decompack;
		decompack=NULL;
		CleanUp7110=NULL;
		Copy7110=NULL;
		*/
	}

	//cache 3's clean-up code
	private void Drop7110Gfx()
	{
		/*
		int i;
		if (Settings.SPC7110)
		{
			Do7110Logging();
		}
		for (i=0;i<MAX_TABLES;i++)
		{
			if (decompack.binfiles[i]!=NULL)
			{
				if (decompack.tableEnts[i].is_file)
				{
					fclose((FILE*)decompack.binfiles[i]);
					decompack.binfiles[i]=NULL;
				}
				else
				{
					delete []decompack.binfiles[i];
					decompack.binfiles[i]=NULL;
				}
			}
		}
		Settings.SPC7110=false;
		Settings.SPC7110RTC=false;
		if (NULL!=decompack)
			delete decompack;
		decompack=NULL;
		CleanUp7110=NULL;
		Copy7110=NULL;
		*/
	}

	//emulate a reset.
	void Spc7110Reset()
	{
		reg4800=0;
		reg4801=0;
		reg4802=0;
		reg4803=0;
		reg4804=0;
		reg4805=0;
		reg4806=0;
		reg4807=0;
		reg4808=0;
		reg4809=0;
		reg480A=0;
		reg480B=0;
		reg480C=0;
		reg4811=0;
		reg4812=0;
		reg4813=0;
		reg4814=0;
		reg4815=0;
		reg4816=0;
		reg4817=0;
		reg4818=0;
		reg4820=0;
		reg4821=0;
		reg4822=0;
		reg4823=0;
		reg4824=0;
		reg4825=0;
		reg4826=0;
		reg4827=0;
		reg4828=0;
		reg4829=0;
		reg482A=0;
		reg482B=0;
		reg482C=0;
		reg482D=0;
		reg482E=0;
		reg482F=0;
		reg4830=0;
		reg4831=0;
		reg4832=1;
		reg4833=2;
		reg4834=0;
		reg4840=0;
		reg4841=0;
		reg4842=0;
		written=0;
		offset_add=0;
		AlignBy=1;
		bank50Internal=0;
		//TODO: memset(bank50,0x00,DECOMP_BUFFER_SIZE);
	}


	//outputs a cumulative log for the game.
	//there's nothing really weird here, just
	//reading the old log, and writing a new one.
	//note the logs are explicitly little-endian, not host byte order.
	private void Do7110Logging()
	{
		/*
		uint8 ent_temp;
		FILE* flog;
		int entries=0;

		if (Settings.SPC7110)
		{
			//flush last read into logging
			(*Copy7110)();

			if (!strncmp((char*)&Memory.ROM [0xffc0], "SUPER POWER LEAG 4   ", 21))
			{
				flog=fopen("spl4-sp7.dat","rb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "MOMOTETSU HAPPY      ",21))
			{
				flog=fopen("smht-sp7.dat","rb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "HU TENGAI MAKYO ZERO ", 21))
			{
				flog=fopen("feoezsp7.dat","rb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "JUMP TENGAIMAKYO ZERO",21))
			{
				flog=fopen("sjumpsp7.dat","rb");
			}
			else
			{
			flog=fopen("misc-sp7.dat","rb");
			}

			if (flog)
			{
				uint8 buffer[8];
				int table=0;
				uint16 offset=0;
				uint16 length=0;
				fseek(flog, 35,0);
				do
				{
					int i=0;
					Data7110 *log=NULL;
					fread(buffer, 1, 8, flog);
					table=buffer[0]|(buffer[1]<<8)|(buffer[2]<<16);
					offset=buffer[6]|(buffer[7]<<8);
					length=buffer[4]|(buffer[5]<<8);
					while (i<MAX_TABLES&&log==NULL)
					{
						if (decompack.tableEnts[i].table==table)
						{
							log=&(decompack.tableEnts[i].location[(buffer[3])]);
							if ((log.used_offset+log.used_len)<(offset+length))
							{
								log.used_offset=offset;
								log.used_len=length;
							}
						}
						i++;
					}
				}
				while (!feof(flog));
				fclose(flog);
			}

			if (!strncmp((char*)&Memory.ROM [0xffc0], "SUPER POWER LEAG 4   ", 21))
			{
				flog=fopen("spl4-sp7.dat","wb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "MOMOTETSU HAPPY      ",21))
			{
				flog=fopen("smht-sp7.dat","wb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "HU TENGAI MAKYO ZERO ", 21))
			{
				flog=fopen("feoezsp7.dat","wb");
			}
			else if (!strncmp((char*)&Memory.ROM [0xffc0], "JUMP TENGAIMAKYO ZERO",21))
			{
				flog=fopen("sjumpsp7.dat","wb");
			}
			else
			{
				flog=fopen("misc-sp7.dat","wb");
			}

			//count entries
			if (flog)
			{
				int j=0;
				int temp=0;
				for (j=0;j<MAX_TABLES;j++)
				{
					for (int k=0;k<256;k++)
					{
						if (decompack.tableEnts[j].location[k].used_len!=0)
							entries++;
					}
				}
				ent_temp=entries&0xFF;
				fwrite(&ent_temp,1,1,flog);
				ent_temp=(entries>>8)&0xFF;
				fwrite(&ent_temp,1,1,flog);
				ent_temp=(entries>>16)&0xFF;
				fwrite(&ent_temp,1,1,flog);
				ent_temp=(entries>>24)&0xFF;
				fwrite(&ent_temp,1,1,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);

				ent_temp=0;
				fwrite(&ent_temp,1,1,flog);
				ent_temp=0;
				fwrite(&ent_temp,1,1,flog);
				ent_temp=0;
				fwrite(&ent_temp,1,1,flog);

				for (j=0;j<MAX_TABLES;j++)
				{
					for (int k=0;k<256;k++)
					{
						if (decompack.tableEnts[j].location[k].used_len!=0)
						{
							ent_temp=decompack.tableEnts[j].table&0xFF;
							fwrite(&ent_temp,1,1,flog);//801
							ent_temp=(decompack.tableEnts[j].table>>8)&0xFF;;
							fwrite(&ent_temp,1,1,flog);//802
							ent_temp=(decompack.tableEnts[j].table>>16)&0xFF;;
							fwrite(&ent_temp,1,1,flog);//803
							ent_temp=k&0xFF;
							fwrite(&ent_temp,1,1,flog);//804
							ent_temp=decompack.tableEnts[j].location[k].used_len&0xFF;
							fwrite(&ent_temp,1,1,flog);//lsb of
							ent_temp=(decompack.tableEnts[j].location[k].used_len>>8)&0xFF;
							fwrite(&ent_temp,1,1,flog);//msb of
							ent_temp=(decompack.tableEnts[j].location[k].used_offset)&0xFF;
							fwrite(&ent_temp,1,1,flog);//lsb of
							ent_temp=(decompack.tableEnts[j].location[k].used_offset>>8)&0xFF;
							fwrite(&ent_temp,1,1,flog);//msb of
						}
					}
				}
				fwrite(&temp,1,4,flog);
				fwrite(&temp,1,4,flog);
				fclose(flog);
			}
		}
		*/
	}

	private void SetSPC7110SRAMMap (int newstate)
	{
		/*
		if ( ( newstate & 0x80 ) != 0 )
		{
			Memory.Map[0x006] = (uint8 *) Memory.MAP_HIROM_SRAM;
			Memory.Map[0x007] = (uint8 *) Memory.MAP_HIROM_SRAM;
			Memory.Map[0x306] = (uint8 *) Memory.MAP_HIROM_SRAM;
			Memory.Map[0x307] = (uint8 *) Memory.MAP_HIROM_SRAM;
		}
		else
		{
			Memory.Map[0x006] = (uint8 *) Memory.MAP_RONLY_SRAM;
			Memory.Map[0x007] = (uint8 *) Memory.MAP_RONLY_SRAM;
			Memory.Map[0x306] = (uint8 *) Memory.MAP_RONLY_SRAM;
			Memory.Map[0x307] = (uint8 *) Memory.MAP_RONLY_SRAM;
		}
		*/
	}

	private boolean SaveSPC7110RTC (SPC7110RTC rtc_f9)
	{
		/*
		FILE* fp;

		if ((fp=fopen(GetFilename(".rtc", SRAM_DIR), "wb"))==NULL)
			return (false);
		int i=0;
		uint8 temp=0;
		for (i=0;i<16;i++)
			fwrite(&rtc_f9.reg[i],1,1,fp);
		temp=rtc_f9.index&0x00FF;
		fwrite(&temp,1,1,fp);
		temp=(rtc_f9.index)>>8;
		fwrite(&temp,1,1,fp);
		temp=(uint8)rtc_f9.control;
		fwrite(&temp,1,1,fp);
		temp=(uint8)rtc_f9.init;
		fwrite(&temp,1,1,fp);
		temp=rtc_f9.last_used&0x00FF;
		fwrite(&temp,1,1,fp);
		temp=(rtc_f9.last_used>>8)&0x00FF;
		fwrite(&temp,1,1,fp);
		temp=(rtc_f9.last_used>>16)&0x00FF;
		fwrite(&temp,1,1,fp);
		temp=(rtc_f9.last_used>>24)&0x00FF;;
		fwrite(&temp,1,1,fp);
		fclose(fp);
		*/
		return true;
	}

	
	private boolean LoadSPC7110RTC (SPC7110RTC rtc_f9)
	{
		/*
		FILE* fp;

		if ((fp=fopen(GetFilename(".rtc", SRAM_DIR), "rb"))==NULL)
			return (false);
		for (int i=0; i<16;i++)
		{
			fread(&(rtc_f9.reg[i]),1,1,fp);
		}
		uint8 temp=0;
		fread(&temp,1,1,fp);
		rtc_f9.index=temp;
		fread(&temp,1,1,fp);
		rtc_f9.index|=(temp<<8);
		fread(&rtc_f9.control,1,1,fp);
		fread(&rtc_f9.init,1,1,fp);

		fread(&temp,1,1,fp);
		rtc_f9.last_used=temp;
		fread(&temp,1,1,fp);
		rtc_f9.last_used|=(temp<<8);
		fread(&temp,1,1,fp);
		rtc_f9.last_used|=(temp<<16);
		fread(&temp,1,1,fp);
		rtc_f9.last_used|=(temp<<24);
		fclose(fp);
		*/
		
		return true;
	}

	

}
