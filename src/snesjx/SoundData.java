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

class SoundData
{
	class AudioChannel
	{
		int state;
	    int type;
	    int volume_left;
	    int volume_right;
	    int hertz;
	    int frequency;
	    int count;
	    boolean loop;
	    int envx;
	    int left_vol_level;
	    int right_vol_level;
	    int envx_target;
	    int env_error;
	    int erate;
	    int direction;
	    int attack_rate;
	    int decay_rate;
	    int sustain_rate;
	    int release_rate;
	    int sustain_level;
	    short sample;
	    short[] decoded = new short[16];
	    short[] previous16 = new short[2];
	    short[] block;
	    int sample_number;
	    boolean last_block;
	    boolean needs_decode;
	    int block_pointer;
	    int sample_pointer;
	    int[] echo_buf_ptr;
	    int mode;
	    int envxx;
	    short next_sample;
	    int interpolate;
	    int[] previous = new int[2];
	    int[] dummy = new int[8];

		int nb_index;
		int[] nb_sample = new int[4];
		int out_sample;
		int xenvx;
		int xenvx_target;
		int xenv_count;
		int xenv_rate;
		int xsmp_count;
		int xattack_rate;
		int xdecay_rate;
		int xsustain_rate;
		int xsustain_level;
	}
	
	static final int SOUND_SAMPLE = 0;
	static final int SOUND_NOISE = 1;

	static final int SOUND_SILENT = 0;
	private static final int SOUND_ATTACK = 1;
	private static final int SOUND_DECAY = 2;
	private static final int SOUND_SUSTAIN = 3;
	private static final int SOUND_RELEASE = 4;
	private static final int SOUND_GAIN = 5;
	private static final int SOUND_INCREASE_LINEAR = 6;
	private static final int SOUND_INCREASE_BENT_LINE = 7;
	private static final int SOUND_DECREASE_LINEAR = 8;
	private static final int SOUND_DECREASE_EXPONENTIAL = 9;

	static final int MODE_NONE = SOUND_SILENT;
	static final int MODE_ADSR = 1;
	static final int MODE_RELEASE = SOUND_RELEASE;
	static final int MODE_GAIN = 5;
	static final int MODE_INCREASE_LINEAR = 6;
	static final int MODE_INCREASE_BENT_LINE = 7;
	static final int MODE_DECREASE_LINEAR = 8;
	static final int MODE_DECREASE_EXPONENTIAL = 9;

	private static final int NUM_CHANNELS = 8;
	private static final int SOUND_DECODE_LENGTH = 16;
	private static final int SOUND_BUFFER_SIZE = (1024 * 16);
	//private static final int MAX_BUFFER_SIZE = SOUND_BUFFER_SIZE;
	//private static final int SOUND_BUFFER_SIZE_MASK = SOUND_BUFFER_SIZE - 1;

	static final int ENV_RANGE = 0x800;
	static final int ENV_MAX = 0x7FF;
	static final int ENV_SHIFT = 4;
	
	private int master_volume_left;
	private int master_volume_right;
	private int echo_volume_left;
	private int echo_volume_right;
	private int echo_feedback;
	private int echo_ptr;
	private int echo_buffer_size;
	private int echo_write_enabled;
	private int pitch_mod;

	private boolean no_filter;
	private int[] master_volume = new int[2];
	private int[] echo_volume = new int[2];

	int noise_rate;
	
	AudioChannel channels[] = new AudioChannel[NUM_CHANNELS];
	
	private static int env_counter_max;
	private static final int	env_counter_max_master = 0x7800;

	private static int rand_seed = 1;
	
	private static int[] temp_hertz = new int[NUM_CHANNELS];
	private static int noise_cache[] = new int[256];
	private static int wave[] = new int[SOUND_BUFFER_SIZE];
	
	private static final	int FIXED_POINT = 0x10000;
	private static final int FIXED_POINT_SHIFT = 16;
	
	private static boolean DoFakeMute = false;
	
	private int[] Loop = new int[16];
	private int[] Echo = new int[24000];
	private int[] FilterTaps = new int[8];

	private int[] MixBuffer = new int[SOUND_BUFFER_SIZE];
	private int[] EchoBuffer = new int[SOUND_BUFFER_SIZE];
	private int[] DummyEchoBuffer = new int[SOUND_BUFFER_SIZE];
	private int FIRIndex = 0;
	
	private static final short[] gauss =
	{
		0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000,
		0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000,
		0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001,
		0x001, 0x001, 0x001, 0x002, 0x002, 0x002, 0x002, 0x002,
		0x002, 0x002, 0x003, 0x003, 0x003, 0x003, 0x003, 0x004,
		0x004, 0x004, 0x004, 0x004, 0x005, 0x005, 0x005, 0x005,
		0x006, 0x006, 0x006, 0x006, 0x007, 0x007, 0x007, 0x008,
		0x008, 0x008, 0x009, 0x009, 0x009, 0x00A, 0x00A, 0x00A,
		0x00B, 0x00B, 0x00B, 0x00C, 0x00C, 0x00D, 0x00D, 0x00E,
		0x00E, 0x00F, 0x00F, 0x00F, 0x010, 0x010, 0x011, 0x011,
		0x012, 0x013, 0x013, 0x014, 0x014, 0x015, 0x015, 0x016,
		0x017, 0x017, 0x018, 0x018, 0x019, 0x01A, 0x01B, 0x01B,
		0x01C, 0x01D, 0x01D, 0x01E, 0x01F, 0x020, 0x020, 0x021,
		0x022, 0x023, 0x024, 0x024, 0x025, 0x026, 0x027, 0x028,
		0x029, 0x02A, 0x02B, 0x02C, 0x02D, 0x02E, 0x02F, 0x030,
		0x031, 0x032, 0x033, 0x034, 0x035, 0x036, 0x037, 0x038,
		0x03A, 0x03B, 0x03C, 0x03D, 0x03E, 0x040, 0x041, 0x042,
		0x043, 0x045, 0x046, 0x047, 0x049, 0x04A, 0x04C, 0x04D,
		0x04E, 0x050, 0x051, 0x053, 0x054, 0x056, 0x057, 0x059,
		0x05A, 0x05C, 0x05E, 0x05F, 0x061, 0x063, 0x064, 0x066,
		0x068, 0x06A, 0x06B, 0x06D, 0x06F, 0x071, 0x073, 0x075,
		0x076, 0x078, 0x07A, 0x07C, 0x07E, 0x080, 0x082, 0x084,
		0x086, 0x089, 0x08B, 0x08D, 0x08F, 0x091, 0x093, 0x096,
		0x098, 0x09A, 0x09C, 0x09F, 0x0A1, 0x0A3, 0x0A6, 0x0A8,
		0x0AB, 0x0AD, 0x0AF, 0x0B2, 0x0B4, 0x0B7, 0x0BA, 0x0BC,
		0x0BF, 0x0C1, 0x0C4, 0x0C7, 0x0C9, 0x0CC, 0x0CF, 0x0D2,
		0x0D4, 0x0D7, 0x0DA, 0x0DD, 0x0E0, 0x0E3, 0x0E6, 0x0E9,
		0x0EC, 0x0EF, 0x0F2, 0x0F5, 0x0F8, 0x0FB, 0x0FE, 0x101,
		0x104, 0x107, 0x10B, 0x10E, 0x111, 0x114, 0x118, 0x11B,
		0x11E, 0x122, 0x125, 0x129, 0x12C, 0x130, 0x133, 0x137,
		0x13A, 0x13E, 0x141, 0x145, 0x148, 0x14C, 0x150, 0x153,
		0x157, 0x15B, 0x15F, 0x162, 0x166, 0x16A, 0x16E, 0x172,
		0x176, 0x17A, 0x17D, 0x181, 0x185, 0x189, 0x18D, 0x191,
		0x195, 0x19A, 0x19E, 0x1A2, 0x1A6, 0x1AA, 0x1AE, 0x1B2,
		0x1B7, 0x1BB, 0x1BF, 0x1C3, 0x1C8, 0x1CC, 0x1D0, 0x1D5,
		0x1D9, 0x1DD, 0x1E2, 0x1E6, 0x1EB, 0x1EF, 0x1F3, 0x1F8,
		0x1FC, 0x201, 0x205, 0x20A, 0x20F, 0x213, 0x218, 0x21C,
		0x221, 0x226, 0x22A, 0x22F, 0x233, 0x238, 0x23D, 0x241,
		0x246, 0x24B, 0x250, 0x254, 0x259, 0x25E, 0x263, 0x267,
		0x26C, 0x271, 0x276, 0x27B, 0x280, 0x284, 0x289, 0x28E,
		0x293, 0x298, 0x29D, 0x2A2, 0x2A6, 0x2AB, 0x2B0, 0x2B5,
		0x2BA, 0x2BF, 0x2C4, 0x2C9, 0x2CE, 0x2D3, 0x2D8, 0x2DC,
		0x2E1, 0x2E6, 0x2EB, 0x2F0, 0x2F5, 0x2FA, 0x2FF, 0x304,
		0x309, 0x30E, 0x313, 0x318, 0x31D, 0x322, 0x326, 0x32B,
		0x330, 0x335, 0x33A, 0x33F, 0x344, 0x349, 0x34E, 0x353,
		0x357, 0x35C, 0x361, 0x366, 0x36B, 0x370, 0x374, 0x379,
		0x37E, 0x383, 0x388, 0x38C, 0x391, 0x396, 0x39B, 0x39F,
		0x3A4, 0x3A9, 0x3AD, 0x3B2, 0x3B7, 0x3BB, 0x3C0, 0x3C5,
		0x3C9, 0x3CE, 0x3D2, 0x3D7, 0x3DC, 0x3E0, 0x3E5, 0x3E9,
		0x3ED, 0x3F2, 0x3F6, 0x3FB, 0x3FF, 0x403, 0x408, 0x40C,
		0x410, 0x415, 0x419, 0x41D, 0x421, 0x425, 0x42A, 0x42E,
		0x432, 0x436, 0x43A, 0x43E, 0x442, 0x446, 0x44A, 0x44E,
		0x452, 0x455, 0x459, 0x45D, 0x461, 0x465, 0x468, 0x46C,
		0x470, 0x473, 0x477, 0x47A, 0x47E, 0x481, 0x485, 0x488,
		0x48C, 0x48F, 0x492, 0x496, 0x499, 0x49C, 0x49F, 0x4A2,
		0x4A6, 0x4A9, 0x4AC, 0x4AF, 0x4B2, 0x4B5, 0x4B7, 0x4BA,
		0x4BD, 0x4C0, 0x4C3, 0x4C5, 0x4C8, 0x4CB, 0x4CD, 0x4D0,
		0x4D2, 0x4D5, 0x4D7, 0x4D9, 0x4DC, 0x4DE, 0x4E0, 0x4E3,
		0x4E5, 0x4E7, 0x4E9, 0x4EB, 0x4ED, 0x4EF, 0x4F1, 0x4F3,
		0x4F5, 0x4F6, 0x4F8, 0x4FA, 0x4FB, 0x4FD, 0x4FF, 0x500,
		0x502, 0x503, 0x504, 0x506, 0x507, 0x508, 0x50A, 0x50B,
		0x50C, 0x50D, 0x50E, 0x50F, 0x510, 0x511, 0x511, 0x512,
		0x513, 0x514, 0x514, 0x515, 0x516, 0x516, 0x517, 0x517,
		0x517, 0x518, 0x518, 0x518, 0x518, 0x518, 0x519, 0x519
	};
	
	static final short[] env_counter_table =
	{
		0x0000, 0x000F, 0x0014, 0x0018, 0x001E, 0x0028, 0x0030, 0x003C,
		0x0050, 0x0060, 0x0078, 0x00A0, 0x00C0, 0x00F0, 0x0140, 0x0180,
		0x01E0, 0x0280, 0x0300, 0x03C0, 0x0500, 0x0600, 0x0780, 0x0A00,
		0x0C00, 0x0F00, 0x1400, 0x1800, 0x1E00, 0x2800, 0x3C00, 0x7800
	};

	private static final short OldAttackRate[] = 
	{
		4100,  2600,  1500,  1000,   640,   380,   260,   160,
		96,	64,	40,	24,	16,	10,	 6,	 1
	};

	private static final short OldDecayRate[] =
	{
		1200,   740,   440,   290,   180,   110,	74,	37
	};

	private static final int OldSustainRate[] =
	{
		~0, 38000, 28000, 24000, 19000, 14000, 12000,  9400,
		7100,  5900,  4700,  3500,  2900,  2400,  1800,  1500,
		1200,   880,   740,   590,   440,   370,   290,   220,
		180,   150,   110,	92,	74,	55,	37,	18
	};

	private static final short OldNoiseFreq[] =
	{
		0,	16,	21,	25,	31,	42,	50,	63,
		84,   100,   125,   167,   200,   250,   333,   400,
		500,   667,   800,  1000,  1300,  1600,  2000,  2700,
		3200,  4000,  5300,  6400,  8000, 10700, 16000, 32000
	};
	
	private Globals globals;
	private Settings settings;

	private APU apu;
	
	class SoundStatus
	{
		int sound_fd;
		int sound_switch;
		int playback_rate;
		int buffer_size;
		int noise_gen;
		boolean mute_sound;
		boolean stereo;
		boolean sixteen_bit;
		boolean encoded;
		
		int samples_mixed_so_far;
		int play_position;
		int err_counter;
		int err_rate;
		
		int stereo_switch;
		double pitch_mul;
	};
	
	SoundStatus so = new SoundStatus();

	void setUp()
	{
		globals = Globals.globals;
		settings = globals.settings;
		apu = globals.apu;
	}
	
	private short G1( int n) { return gauss[256 + n]; }
	private short G2( int n) { return gauss[512 + n]; }
	private short G3( int n) { return gauss[255 + n]; }
	private short G4( int n) { return gauss[n - 1]; }

	private void APUSetEndOfSample (int i, AudioChannel ch)
	{
		ch.state = SOUND_SILENT;
		ch.mode = MODE_NONE;
		ch.out_sample = 0;
		ch.xenvx = 0;
		
		if( ! DoFakeMute )
		{
			apu.DSP[APU.APU_ENDX] |=   1 << i;
			apu.DSP[APU.APU_KON]  &= ~(1 << i);
			apu.DSP[APU.APU_KOFF] &= ~(1 << i);
			apu.KeyedChannels &= ~(1 << i);
		}
	}

	private void APUSetEndX (int i)
	{
		if( ! DoFakeMute )
		{
			apu.DSP[APU.APU_ENDX] |= 1 << i;
		}
	}
	
	private void SetEnvRate (AudioChannel ch, int rate_count, int xtarget)
	{
		ch.xenvx_target = xtarget;
		ch.xenv_rate = rate_count;
	}
	
	void SetEnvelopeRate(int channel, int rate_count, int xtarget)
	{
		SetEnvRate (channels[channel], rate_count, xtarget);
	}
	
	void SetSoundVolume(int channel, int volume_left, int volume_right)
	{
		AudioChannel ch = channels[channel];

		if ( ( so.stereo_switch + 1 ) > 0)
		{
			volume_left  = ((so.stereo_switch & (  1 << channel)) > 0 ? volume_left  : 0);
			volume_right = ((so.stereo_switch & (256 << channel)) > 0 ? volume_right : 0);
		}

		if ( so.stereo == false)
		{
			volume_left = (Math.abs(volume_right) + Math.abs(volume_left)) >> 1;
		}

		ch.volume_left  = volume_left;
		ch.volume_right = volume_right;
	}
	
	void SetMasterVolume (int volume_left, int volume_right)
	{
		if (settings.DisableMasterVolume)
		{
			master_volume_left  = 127;
			master_volume_right = 127;
			master_volume[0] = master_volume[1] = 127;
		}
		else
		{
			if ( so.stereo == false)
				volume_left = (Math.abs(volume_right) + Math.abs(volume_left)) >> 1;

			master_volume_left = volume_left;
			master_volume_right = volume_right;
			master_volume[0] = volume_left;
			master_volume[1] = volume_right;
		}
	}
	
	void SetEchoVolume(int volume_left, int volume_right)
	{
		if ( so.stereo == false)
			volume_left = (Math.abs(volume_right) + Math.abs(volume_left)) >> 1;

		echo_volume_left  = volume_left;
		echo_volume_right = volume_right;
		echo_volume[0]	 = volume_left;
		echo_volume[1] = volume_right;
	}
	
	void SetEchoEnable(int Byte)
	{

		for (int i = 0; i < NUM_CHANNELS; i++)
		{
			if ( ( Byte & (1 << i) ) > 0 )
				channels[i].echo_buf_ptr = EchoBuffer;
			else
				channels[i].echo_buf_ptr = DummyEchoBuffer;
		}
	}

	void SetEchoFeedback(int feedback)
	{
		echo_feedback = feedback;
	}

	void SetEchoDelay (int delay)
	{
		echo_buffer_size = (delay << 10) * so.playback_rate / 32000;
		if (so.stereo == false)
			echo_buffer_size >>= 1;
		if (echo_buffer_size > 0)
			echo_ptr %= echo_buffer_size;
		else
			echo_ptr = 0;
	}

	void SetEchoWriteEnable (int Byte)
	{
		echo_write_enabled = Byte;
	}

	void SetFrequencyModulationEnable (int Byte)
	{
		pitch_mod = Byte & ~1;
	}

	void SetSoundKeyOff (int channel)
	{
		AudioChannel ch = channels[channel];

		if (ch.state != SOUND_SILENT)
		{
			ch.state = SOUND_RELEASE;
			ch.mode = MODE_RELEASE;
			SetEnvRate (ch, env_counter_max, 0);
		}
	}

	private void PrepareSoundForSnapshotSave (boolean restore)
	{
		int i, j;

		if (!restore)
		{
			for (i = 0; i < NUM_CHANNELS; i++)
			{
				AudioChannel ch = channels[i];

				ch.count = 0;
				ch.envx = ch.xenvx >> 4;
				ch.envx_target = ch.xenvx_target >> 4;
				ch.direction = 0;
				ch. left_vol_level = (ch.xenvx * ch.volume_left ) >> 11;
				ch.right_vol_level = (ch.xenvx * ch.volume_right) >> 11;
				ch.release_rate = 8;
				ch.sustain_level = ch.xsustain_level >> 8;

				if (env_counter_max < ch.xenv_count)
					ch.env_error = 0;
				else
					ch.env_error = (int)((double) FIXED_POINT / env_counter_max * (env_counter_max - ch.xenv_count));

				if (ch.xenv_rate < 0)
					ch.erate = 0;
				else
					ch.erate = (int)((double) FIXED_POINT / env_counter_max * ch.xenv_rate);

				for (j = 0; j < 32; j++)
					if (env_counter_table[j] == ch.xattack_rate)
						break;
				ch.attack_rate  = OldAttackRate[(((j - 1) >> 1) & 0xF)];

				for (j = 0; j < 32; j++)
					if (env_counter_table[j] == ch.xdecay_rate)
						break;
				ch.decay_rate   = OldDecayRate[(((j - 0x10) >> 1) & 0x7)];

				for (j = 0; j < 32; j++)
					if (env_counter_table[j] == ch.xsustain_rate)
						break;
				ch.sustain_rate = OldSustainRate[(j & 0x1F)];
			}

			for (j = 0; j < 32; j++)
				if (env_counter_table[j] == noise_rate)
					break;

			for (i = 0; i < NUM_CHANNELS; i++)
			{
				AudioChannel ch = channels[i];

				temp_hertz[i] = ch.hertz;
				if (ch.type == SOUND_NOISE)
					ch.hertz = OldNoiseFreq[(j & 0x1F)];
			}
		}
		else
		{
			for (i = 0; i < NUM_CHANNELS; i++)
			{
				AudioChannel ch = channels[i];

				ch.hertz = temp_hertz[i];
			}
		}
	}

	private void ConvertSoundOldValues ()
	{
		int i, j;
		int old_noise_freq = 0;

		for (i = 0; i < NUM_CHANNELS; i++)
		{
			AudioChannel ch = channels[i];

			ch.xenvx = ch.envx << 4;
			ch.xenvx_target = ch.envx_target << 4;
			ch.out_sample = ((ch.sample * ch.xenvx) >> 11) & ~1;
			ch.xsustain_level = ch.sustain_level << 8;

			ch.xenv_rate  = (int) ((double) ch.erate	 * env_counter_max / FIXED_POINT);
			ch.xenv_count = env_counter_max -
							 (int) ((double) ch.env_error * env_counter_max / FIXED_POINT);

			for (j = 0; j < 16; j++)
				if (OldAttackRate[j]  == ch.attack_rate)
					break;
			ch.xattack_rate  = env_counter_table[(((j << 1) + 1) & 0x1F)];

			for (j = 0; j <  8; j++)
				if (OldDecayRate[j]   == ch.decay_rate)
					break;
			ch.xdecay_rate   = env_counter_table[ (((j << 1) + 0x10) & 0x1F)];

			for (j = 0; j < 32; j++)
				if (OldSustainRate[j] == ch.sustain_rate)
					break;
			ch.xsustain_rate = env_counter_table[(j & 0x1F)];

			if (ch.type == SOUND_NOISE)
			{
				old_noise_freq = ch.hertz;
				ch.hertz = 32000;
			}
		}

		if (old_noise_freq > 0)
		{
			for (j = 0; j < 32; j++)
				if (OldNoiseFreq[j] == old_noise_freq)
					break;
			noise_rate = env_counter_table[(j & 0x1F)];
		}
		else
			noise_rate = 0;
	}

	private void FixSoundAfterSnapshotLoad (int version)
	{
		SetEchoEnable( apu.DSP[APU.APU_EON] );
		
		SetEchoWriteEnable( (apu.DSP[APU.APU_FLG] & APU.APU_ECHO_DISABLED) > 0 ? 1 : 0 );
		
		SetEchoDelay( apu.DSP[APU.APU_EDL] & 0xF );
		SetEchoFeedback( apu.DSP[APU.APU_EFB] );

		SetFilterCoefficient(0, apu.DSP[APU.APU_C0]);
		SetFilterCoefficient(1, apu.DSP[APU.APU_C1]);
		SetFilterCoefficient(2, apu.DSP[APU.APU_C2]);
		SetFilterCoefficient(3, apu.DSP[APU.APU_C3]);
		SetFilterCoefficient(4, apu.DSP[APU.APU_C4]);
		SetFilterCoefficient(5, apu.DSP[APU.APU_C5]);
		SetFilterCoefficient(6, apu.DSP[APU.APU_C6]);
		SetFilterCoefficient(7, apu.DSP[APU.APU_C7]);

		if (version < 2)
			ConvertSoundOldValues ();

		for (int i = 0; i < NUM_CHANNELS; i++)
		{
			SetSoundFrequency (i, channels[i].hertz);
			channels[i].needs_decode = true;
			channels[i].nb_index = 0;
			channels[i].nb_sample[0] = 0;
			channels[i].nb_sample[1] = 0;
			channels[i].nb_sample[2] = 0;
			channels[i].nb_sample[3] = 0;
			channels[i].xsmp_count = 0;
			channels[i].previous[0] = channels[i].previous16[0];
			channels[i].previous[1] = channels[i].previous16[1];
		}

		master_volume[0]	 = master_volume_left;
		master_volume[1] = master_volume_right;
		echo_volume[0]	 = echo_volume_left;
		echo_volume[1] = echo_volume_right;
	}

	void SetFilterCoefficient (int tap, int value)
	{		
		FilterTaps[tap & 7] = value;
		no_filter =
			FilterTaps[0] == 127 &&
			FilterTaps[1] == 0   &&
			FilterTaps[2] == 0   &&
			FilterTaps[3] == 0   &&
			FilterTaps[4] == 0   &&
			FilterTaps[5] == 0   &&
			FilterTaps[6] == 0   &&
			FilterTaps[7] == 0;
	}

	void SetSoundADSR(int channel, int ar, int dr, int sr, int sl)
	{
		AudioChannel ch = channels[channel];

		ch.xattack_rate   = env_counter_table[(ar << 1) + 1];
		ch.xdecay_rate	= env_counter_table[(dr << 1) + 0x10];
		ch.xsustain_rate  = env_counter_table[sr];
		ch.xsustain_level = (ENV_RANGE >> 3) * (sl + 1);

		switch (ch.state)
		{
			case SOUND_ATTACK:
				SetEnvRate (ch, ch.xattack_rate, ENV_MAX);
				break;

			case SOUND_DECAY:
				SetEnvRate (ch, ch.xdecay_rate, ch.xsustain_level);
				break;

			case SOUND_SUSTAIN:
				SetEnvRate (ch, ch.xsustain_rate, 0);
				break;
		}
	}

	void SetEnvelopeHeight (int channel, int xlevel)
	{
		AudioChannel ch = channels[channel];

		ch.xenvx = ch.xenvx_target = xlevel;
		ch.xenv_rate = 0;
		if (xlevel == 0 && ch.state != SOUND_SILENT && ch.state != SOUND_GAIN)
			APUSetEndOfSample (channel, ch);
	}

	int GetEnvelopeHeight(int channel)
	{
		if (settings.SoundEnvelopeHeightReading)
			return ((channels[channel].xenvx >> ENV_SHIFT) & 0x7F);
		else
			return (0);
	}

	void SetSoundFrequency (int channel, int hertz)
	{
		if (so.playback_rate > 0)
		{
			channels[channel].frequency = (int)
				((long) (hertz << (FIXED_POINT_SHIFT - 15)) * 32000 / so.playback_rate);
		}
		if (settings.FixFrequency)
		{
			channels[channel].frequency = (int)	(channels[channel].frequency * so.pitch_mul);
		}
	}

	void SetSoundHertz (int channel, int hertz)
	{
		channels[channel].hertz = hertz;
		SetSoundFrequency (channel, hertz);
	}

	void SetSoundType(int channel, int type_of_sound)
	{
		channels[channel].type = type_of_sound;
	}

	boolean SetSoundMute(boolean mute)
	{
		boolean old = so.mute_sound;
		so.mute_sound = mute;
		return (old);
	}

	private void AltDecodeBlock(AudioChannel ch)
	{
		int out;
		int shift;
		int sample1, sample2;
		int i;
		
		if (ch.block_pointer > 0x10000 - 9)
		{
			ch.last_block = true;
			ch.loop = false;
			ch.block = ch.decoded;
			
			for (int j = 0; j < ch.decoded.length; j++)
				ch.decoded[j] = 0;
			
			return;
		}

		ByteArrayOffset compressed = apu.RAM.getOffsetBuffer(ch.block_pointer);

		int filter = compressed.get8Bit(0);
		ch.last_block = (filter & 1) > 0;
		ch.loop = (filter & 2) != 0;

		int raw_position = 0;
		short[] raw = ch.decoded;
		ch.block = ch.decoded;

		compressed.setOffset(compressed.getOffset() + 1);

		int prev0 = ch.previous[0];
		int prev1 = ch.previous[1];
		shift = filter >> 4;

		switch ((filter >> 2) & 3)
		{
			case 0:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;
					
					raw[raw_position++] = (short) ( sample1 << shift);
					raw[raw_position++] = (short) ( sample1 << shift);
				}
				prev1 = raw[raw_position - 2];
				prev0 = raw[raw_position - 1];
				break;

			case 1:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;
					prev0 = (short) prev0;
					
					prev1 = (sample1 << shift) + prev0 - (prev0 >> 4);
					
					raw[raw_position++] = (short) prev1;
					prev1 = (short) prev1;
					
					prev0 = (sample2 << shift) + prev1 - (prev1 >> 4);
					raw[raw_position++] = (short) prev0;
				}
				break;

			case 2:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;

					out = (sample1 << shift) - prev1 + (prev1 >> 4);
					prev1 = (short) prev0;
					prev0 &= ~3;
					
					prev0 = out + (prev0 << 1) - (prev0 >> 5) - (prev0 >> 4);
					raw[raw_position++] = (short) prev0;

					out = (sample2 << shift) - prev1 + (prev1 >> 4);
					prev1 = (short) prev0;
					prev0 &= ~3;
					
					prev0 = out + (prev0 << 1) - (prev0 >> 5) - (prev0 >> 4);
					raw[raw_position++] = (short) prev0;
				}
				break;

			case 3:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;
					out = (sample1 << shift);

					out = out - prev1 + (prev1 >> 3) + (prev1 >> 4);
					prev1 = (short) prev0;
					prev0 &= ~3;
					prev0 = out + (prev0 << 1) - (prev0 >> 3) - (prev0 >> 4) - (prev1 >> 6);
					raw[raw_position++] = (short) prev0;

					out = (sample2 << shift);
					out = out - prev1 + (prev1 >> 3) + (prev1 >> 4);
					prev1 = (short) prev0;
					prev0 &= ~3;
					prev0 = out + (prev0 << 1) - (prev0 >> 3) - (prev0 >> 4) - (prev1 >> 6);
					raw[raw_position++] = (short) prev0;
				}
				break;
		}

		ch.previous[0] = prev0;
		ch.previous[1] = prev1;

		ch.block_pointer += 9;
	}

	private void AltDecodeBlock2 (AudioChannel ch)
	{
		int out;
		int shift;
		int sample1, sample2;
		int i;

		if (ch.block_pointer > 0x10000 - 9)
		{
			ch.last_block = true;
			ch.loop = false;
			ch.block = ch.decoded;
			for (int j = 0; j < ch.decoded.length; j++)
				ch.decoded[j] = 0;
			return;
		}

		ByteArrayOffset compressed = apu.RAM.getOffsetBuffer(ch.block_pointer);

		int filter = compressed.get8Bit(0);
		ch.last_block = (filter & 1) > 0;
		ch.loop = (filter & 2) != 0;
		
		int raw_position = 0;
		short[] raw = ch.decoded;
		ch.block = ch.decoded;
		
		compressed.setOffset(compressed.getOffset() + 1);

		shift = filter >> 4;
		int prev0 = ch.previous[0];
		int prev1 = ch.previous[1];

		if(shift > 12)
			shift -= 4;

		switch ((filter >> 2) & 3)
		{
			case 0:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;

					out = (sample1 << shift);

					prev1 = prev0;
					prev0 = out;
					out = CLIP16(out);
					raw[raw_position++] = (short) out;

					out = (sample2 << shift);

					prev1 = prev0;
					prev0 = out;
					out = CLIP16(out);
					raw[raw_position++] = (short) out;
				}
				break;

			case 1:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;
					out = (sample1 << shift);
					out += (int) ((double) prev0 * 15/16);

					prev1 = prev0;
					prev0 = out;
					out = CLIP16(out);
					raw[raw_position++] = (short) out;

					out = (sample2 << shift);
					out += (int) ((double) prev0 * 15/16);

					prev1 = prev0;
					prev0 = out;
					out = CLIP16(out);
					raw[raw_position++] = (short) out;
				}
				break;

			case 2:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;

					out = ((sample1 << shift) * 256 + (prev0 & ~0x2) * 488 - prev1 * 240) >> 8;

					prev1 = prev0;
					prev0 = (short) out;
					raw[raw_position++] = (short) out;

					out = ((sample2 << shift) * 256 + (prev0 & ~0x2) * 488 - prev1 * 240) >> 8;

					prev1 = prev0;
					prev0 = (short) out;
					raw[raw_position++] = (short) out;
				}
				break;

			case 3:
				for (i = 8; i != 0; i--)
				{
					sample1 = compressed.get8Bit(0) + 1;
					compressed.put8Bit(0, sample1);
					
					sample2 = sample1 << 4;
					sample2 >>= 4;
					sample1 >>= 4;
					out = (sample1 << shift);
					out += (int) ((double) prev0 * 115/64 - (double) prev1 * 13/16);

					prev1 = prev0;
					prev0 = out;

					out = CLIP16(out);
					raw[raw_position++] = (short) out;

					out = (sample2 << shift);
					out += (int) ((double) prev0 * 115/64 - (double) prev1 * 13/16);

					prev1 = prev0;
					prev0 = out;

					out = CLIP16(out);
					raw[raw_position++] = (short) out;
				}
				break;
		}

		ch.previous[0] = prev0;
		ch.previous[1] = prev1;

		ch.block_pointer += 9;
	}

	private void DecodeBlock (AudioChannel ch)
	{
		int out;
		int shift;
		byte sample1, sample2;
		boolean invalid_header;

		// NAC: Remove this and the methods?
		if( settings.AltSampleDecode > 0 )
		{
			if (settings.AltSampleDecode < 3)
				AltDecodeBlock (ch);
			else
				AltDecodeBlock2 (ch);
			return;
		}

		if (ch.block_pointer > 0x10000 - 9)
		{
			ch.last_block = true;
			ch.loop = false;
			ch.block = ch.decoded;
			return;
		}

		ByteArrayOffset compressed = apu.RAM.getOffsetBuffer(ch.block_pointer);

		int filter = compressed.get8Bit(0);
		ch.last_block = (filter & 1) > 0;
		ch.loop = (filter & 2) != 0;

		compressed.setOffset(compressed.getOffset() + 1);
		
		int decoded_position = 0;
		ch.block = ch.decoded;

		// Seperate out the header parts used for decoding
		shift = filter >> 4;

		// Header validity check: if range(shift) is over 12, ignore
		// all bits of the data for that block except for the sign bit of each
		invalid_header = !(shift < 0xD);

		filter &= 0x0C;

		int prev0 = ch.previous[0];
		int prev1 = ch.previous[1];

		for (int i = 8; i != 0; i--)
		{
			// Get byte direct
			sample1 = compressed.buffer[compressed.getOffset()];
				
			compressed.setOffset(compressed.getOffset() + 1);
			
			sample2 = (byte) (sample1 << 4);
			
			//Sample 2 = Bottom Nibble, Sign Extended.
			sample2 >>= 4;
			
			//Sample 1 = Top Nibble, shifted down and Sign Extended.
			sample1 >>= 4;

			for (int nybblesmp = 0; nybblesmp < 2; nybblesmp++)
			{
				out = (nybblesmp > 0 ? sample2 : sample1);
				if (!invalid_header)
				{
					out = (out << shift) >> 1;
				}
				else
				{
					out &= ~0x7FF;
				}

				switch (filter)
				{
					case 0x00:
						// Method0 -[Smp]
						break;

					case 0x04:
						// Method1 -[Delta]+[Smp-1](15/16)
						out += prev0 >> 1;
						out += (-prev0) >> 5;
						break;

					case 0x08:
						// Method2 -[Delta]+[Smp-1](61/32)-[Smp-2](15/16)
						out += prev0;
						out += (-(prev0 + (prev0 >> 1))) >> 5;
						out -= prev1 >> 1;
						out += prev1 >> 5;
						break;

					case 0x0C:
						// Method3 -[Delta]+[Smp-1](115/64)-[Smp-2](13/16)
						out += prev0;
						out += (-(prev0 + (prev0 << 2) + (prev0 << 3))) >> 7;
						out -= prev1 >> 1;
						out += (prev1 + (prev1 >> 1)) >> 4;
						break;
				}

				out = CLIP16(out);

				prev1 = (short) prev0;
				prev0 = ch.decoded[decoded_position++] = (short) (out << 1);
			}
		}

		ch.previous[0] = prev0;
		ch.previous[1] = prev1;

		ch.block_pointer += 9;
	}

	private void MixStereo (int sample_count)
	{
		DoFakeMute = settings.FakeMuteFix;

		int pitch_mod = this.pitch_mod & ~apu.DSP[APU.APU_NON];

		int noise_index = 0;
		int noise_count = 0;
		
		boolean stereo_exit = false;

		if (apu.DSP[APU.APU_NON] > 0)
		{
			for (int I = 0; I < sample_count; I += 2)
			{
				noise_count -= noise_rate;
				while (noise_count <= 0)
				{
					rand_seed = rand_seed * 48828125 + 1;
					noise_cache[noise_index] = rand_seed;
					noise_index = (noise_index + 1) & 0xFF;
					noise_count += env_counter_max;
				}
			}
		}

		for (int J = 0; J < NUM_CHANNELS; J++)
		{
			AudioChannel ch = channels[J];
			int freq = ch.frequency;

			boolean last_block = false;

			if (ch.type == SOUND_NOISE)
			{
				noise_index = 0;
			}

			if (ch.state == SOUND_SILENT || last_block || (so.sound_switch & (1 << J)) == 0 )
				continue;

			boolean mod1 = ( pitch_mod & (1 << J) ) > 0;
			boolean mod2 = ( pitch_mod & (1 << (J + 1))) > 0;

			if (ch.needs_decode)
			{
				DecodeBlock(ch);
				ch.needs_decode = false;
				ch.sample = ch.block[0];
				ch.sample_pointer = 0;
			}

			for (int I = 0; I < sample_count; I += 2)
			{
				switch (ch.state)
				{
					case SOUND_ATTACK:
						if (ch.xenv_rate == env_counter_max_master)
							ch.xenvx += (ENV_RANGE >> 1); // FIXME
						else
						{
							ch.xenv_count -= ch.xenv_rate;
							while (ch.xenv_count <= 0)
							{
								ch.xenvx += (ENV_RANGE >> 6); // 1/64
								ch.xenv_count += env_counter_max;
							}
						}

						if (ch.xenvx > ENV_MAX)
						{
							ch.xenvx = ENV_MAX;

							if (ch.xsustain_level != ENV_RANGE)
							{
								ch.state = SOUND_DECAY;
								SetEnvRate (ch, ch.xdecay_rate, ch.xsustain_level);
							}
							else
							{
								ch.state = SOUND_SUSTAIN;
								SetEnvRate (ch, ch.xsustain_rate, 0);
							}
						}

						break;

					case SOUND_DECAY:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1; // 1 - 1/256
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx <= ch.xenvx_target)
						{
							if (ch.xenvx <= 0)
							{
								APUSetEndOfSample (J, ch);
								stereo_exit = true;
							}
							else
							{
								ch.state = SOUND_SUSTAIN;
								SetEnvRate (ch, ch.xsustain_rate, 0);
							}
						}

						break;

					case SOUND_SUSTAIN:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1;  // 1 - 1/256
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx <= 0)
						{
							APUSetEndOfSample (J, ch);
							stereo_exit = true;
						}

						break;

					case SOUND_RELEASE:
						ch.xenv_count -= env_counter_max;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx -= (ENV_RANGE >> 8); // 1/256
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx <= 0)
						{
							APUSetEndOfSample (J, ch);
							stereo_exit = true;
						}

						break;

					case SOUND_INCREASE_LINEAR:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx += (ENV_RANGE >> 6); // 1/64
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx > ENV_MAX)
						{
							ch.xenvx = ENV_MAX;
							ch.state = SOUND_GAIN;
							ch.mode  = MODE_GAIN;
							SetEnvRate (ch, 0, 0);
						}

						break;

					case SOUND_INCREASE_BENT_LINE:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							if (ch.xenvx >= ((ENV_RANGE * 3) >> 2)) // 0x600
								ch.xenvx += (ENV_RANGE >> 8); // 1/256
							else
								ch.xenvx += (ENV_RANGE >> 6); // 1/64

							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx > ENV_MAX)
						{
							ch.xenvx = ENV_MAX;
							ch.state = SOUND_GAIN;
							ch.mode  = MODE_GAIN;
							SetEnvRate (ch, 0, 0);
						}

						break;

					case SOUND_DECREASE_LINEAR:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx -= (ENV_RANGE >> 6); // 1/64
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx <= 0)
						{
							APUSetEndOfSample (J, ch);
							stereo_exit = true;
						}

						break;

					case SOUND_DECREASE_EXPONENTIAL:
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1; // 1 - 1/256
							ch.xenv_count += env_counter_max;
						}

						if (ch.xenvx <= 0)
						{
							APUSetEndOfSample (J, ch);
							stereo_exit = true;
						}

						break;

					case SOUND_GAIN:
						SetEnvRate (ch, 0, 0);

						break;
				}
				
				if (stereo_exit) break;

				ch.xsmp_count += mod1 ? (((long) freq * (32768 + wave[I >> 1])) >> 15) : freq;

				while (ch.xsmp_count >= 0)
				{
					ch.xsmp_count -= FIXED_POINT;
					ch.nb_sample[ch.nb_index] = ch.sample;
					ch.nb_index = (ch.nb_index + 1) & 3;

					ch.sample_pointer++;
					if (ch.sample_pointer == SOUND_DECODE_LENGTH)
					{
						ch.sample_pointer = 0;

						if (ch.last_block)
						{
							APUSetEndX (J);
							if (!ch.loop)
							{
								ch.xenvx = 0;
								last_block = true;
								//APUSetEndOfSample (J, ch);
								while (ch.xsmp_count >= 0)
								{
									ch.xsmp_count -= FIXED_POINT;
									ch.nb_sample[ch.nb_index] = 0;
									ch.nb_index = (ch.nb_index + 1) & 3;
								}

								break;
							}
							else
							{
								ch.last_block = false;
								ByteArrayOffset dir = apu.GetSampleAddress (ch.sample_number);
								ch.block_pointer = dir.get16Bit(2);
							}
						}

						DecodeBlock (ch);
					}

					ch.sample = ch.block[ch.sample_pointer];
				}

				int outx, d;

				if (ch.type == SOUND_SAMPLE)
				{
					if (settings.InterpolatedSound)
					{
						// 4-point gaussian interpolation
						d = ch.xsmp_count >> (FIXED_POINT_SHIFT - 8);
						outx  = ((G4(-d) * ch.nb_sample[ ch.nb_index		 ]) >> 11) & ~1;
						outx += ((G3(-d) * ch.nb_sample[(ch.nb_index + 1) & 3]) >> 11) & ~1;
						outx += ((G2( d) * ch.nb_sample[(ch.nb_index + 2) & 3]) >> 11) & ~1;
						outx = ((outx & 0xFFFF) ^ 0x8000) - 0x8000;
						outx += ((G1( d) * ch.nb_sample[(ch.nb_index + 3) & 3]) >> 11) & ~1;
						outx = CLIP16(outx);
					}
					else
						outx = ch.sample;
				}
				else // SAMPLE_NOISE
				{
					noise_count -= noise_rate;
					while (noise_count <= 0)
					{
						noise_count += env_counter_max;
						noise_index = (noise_index + 1) & 0xFF;
					}

					outx = noise_cache[noise_index] >> 16;
				}

				outx = ((outx * ch.xenvx) >> 11) & ~1;
				ch.out_sample = outx;

				if (mod2)
					wave[I >> 1] = outx;

				int VL, VR;

				VL = (outx * ch.volume_left ) >> 7;
				VR = (outx * ch.volume_right) >> 7;

				MixBuffer[I ^ ( 0 ) ] += VL;
				MixBuffer[I + ( 1) ] += VR;
				ch.echo_buf_ptr[I ^ ( 0 ) ] += VL;
				ch.echo_buf_ptr[I + ( 1 ) ] += VR;
			}

			stereo_exit = false;
		}
		
		DoFakeMute = false;
	}

	private void MixMono (int sample_count)
	{
		DoFakeMute = settings.FakeMuteFix;

		int pitch_mod = this.pitch_mod & ~apu.DSP[APU.APU_NON];
		
		boolean mono_exit = false;

		int noise_index = 0;
		int noise_count = 0;

		if (apu.DSP[APU.APU_NON] > 0)
		{
			for (int I = 0; I < (int) sample_count; I++)
			{
				noise_count -= noise_rate;
				while (noise_count <= 0)
				{
					rand_seed = rand_seed * 48828125 + 1;
					noise_cache[noise_index] = rand_seed;
					noise_index = (noise_index + 1) & 0xFF;
					noise_count += env_counter_max;
				}
			}
		}

		for (int J = 0; J < NUM_CHANNELS; J++)
		{
			AudioChannel ch = channels[J];
			int freq = ch.frequency;

			boolean last_block = false;

			if (ch.type == SOUND_NOISE)
			{
				noise_index = 0;
			}

			if (ch.state == SOUND_SILENT || last_block || (so.sound_switch & (1 << J)) == 0)
				continue;

			boolean mod1 = ( pitch_mod & (1 << J) ) > 0;
			boolean mod2 = ( pitch_mod & (1 << (J + 1)) ) > 0;

			if (ch.needs_decode)
			{
				DecodeBlock(ch);
				ch.needs_decode = false;
				ch.sample = ch.block[0];
				ch.sample_pointer = 0;
			}

			for (int I = 0; I < sample_count; I++)
			{
				switch (ch.state)
				{
				case SOUND_ATTACK:
					if (ch.xenv_rate == env_counter_max_master)
					{
						ch.xenvx += (ENV_RANGE >> 1); // FIXME
					}
					else
					{
						ch.xenv_count -= ch.xenv_rate;
						while (ch.xenv_count <= 0)
						{
							ch.xenvx += (ENV_RANGE >> 6); // 1/64
							ch.xenv_count += env_counter_max;
						}
					}

					if (ch.xenvx > ENV_MAX)
					{
						ch.xenvx = ENV_MAX;

						if (ch.xsustain_level != ENV_RANGE)
						{
							ch.state = SOUND_DECAY;
							SetEnvRate (ch, ch.xdecay_rate, ch.xsustain_level);
						}
						else
						{
							ch.state = SOUND_SUSTAIN;
							SetEnvRate (ch, ch.xsustain_rate, 0);
						}
					}

					break;

				case SOUND_DECAY:
					ch.xenv_count -= ch.xenv_rate;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1; // 1 - 1/256
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx <= ch.xenvx_target)
					{
						if (ch.xenvx <= 0)
						{
							APUSetEndOfSample (J, ch);
							mono_exit = true;
						}
						else
						{
							ch.state = SOUND_SUSTAIN;
							SetEnvRate(ch, ch.xsustain_rate, 0);
						}
					}

					break;

				case SOUND_SUSTAIN:
					ch.xenv_count -= ch.xenv_rate;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1;  // 1 - 1/256
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx <= 0)
					{
						APUSetEndOfSample(J, ch);
						mono_exit = true;
					}

					break;

				case SOUND_RELEASE:
					ch.xenv_count -= env_counter_max;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx -= (ENV_RANGE >> 8); // 1/256
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx <= 0)
					{
						APUSetEndOfSample(J, ch);
						mono_exit = true;
					}

					break;

				case SOUND_INCREASE_LINEAR:
					ch.xenv_count -= ch.xenv_rate;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx += (ENV_RANGE >> 6); // 1/64
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx > ENV_MAX)
					{
						ch.xenvx = ENV_MAX;
						ch.state = SOUND_GAIN;
						ch.mode  = MODE_GAIN;
						SetEnvRate(ch, 0, 0);
					}

					break;

				case SOUND_INCREASE_BENT_LINE:
					ch.xenv_count -= ch.xenv_rate;
					
					while (ch.xenv_count <= 0)
					{
						if (ch.xenvx >= ((ENV_RANGE * 3) >> 2)) // 0x600
						{
							ch.xenvx += (ENV_RANGE >> 8); // 1/256
						}
						else
						{
							ch.xenvx += (ENV_RANGE >> 6); // 1/64
						}

						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx > ENV_MAX)
					{
						ch.xenvx = ENV_MAX;
						ch.state = SOUND_GAIN;
						ch.mode  = MODE_GAIN;
						SetEnvRate (ch, 0, 0);
					}

					break;

				case SOUND_DECREASE_LINEAR:
					ch.xenv_count -= ch.xenv_rate;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx -= (ENV_RANGE >> 6); // 1/64
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx <= 0)
					{
						APUSetEndOfSample(J, ch);
						mono_exit = true;
					}

					break;

				case SOUND_DECREASE_EXPONENTIAL:
					ch.xenv_count -= ch.xenv_rate;
					while (ch.xenv_count <= 0)
					{
						ch.xenvx -= ((ch.xenvx - 1) >> 8) + 1; // 1 - 1/256
						ch.xenv_count += env_counter_max;
					}

					if (ch.xenvx <= 0)
					{
						APUSetEndOfSample(J, ch);
						mono_exit = true;
					}

					break;

				case SOUND_GAIN:
					SetEnvRate(ch, 0, 0);

					break;
				}
				
				if (mono_exit) break;

				ch.xsmp_count += mod1 ? (((long) freq * (32768 + wave[I])) >> 15) : freq;

				while (ch.xsmp_count >= 0)
				{
					ch.xsmp_count -= FIXED_POINT;
					ch.nb_sample[ch.nb_index] = ch.sample;
					ch.nb_index = (ch.nb_index + 1) & 3;

					ch.sample_pointer++;
					
					if (ch.sample_pointer == SOUND_DECODE_LENGTH)
					{
						ch.sample_pointer = 0;

						if (ch.last_block)
						{
							APUSetEndX(J);
							
							if ( ! ch.loop )
							{
								ch.xenvx = 0;
								last_block = true;
								//APUSetEndOfSample (J, ch);
								while (ch.xsmp_count >= 0)
								{
									ch.xsmp_count -= FIXED_POINT;
									ch.nb_sample[ch.nb_index] = 0;
									ch.nb_index = (ch.nb_index + 1) & 3;
								}

								break;
							}
							else
							{
								ch.last_block = false;
								ByteArrayOffset dir = apu.GetSampleAddress(ch.sample_number);
								ch.block_pointer = dir.get16Bit(2);
							}
						}

						DecodeBlock(ch);
					}

					ch.sample = ch.block[ch.sample_pointer];
				}

				int outx, d;

				if (ch.type == SOUND_SAMPLE)
				{
					if (settings.InterpolatedSound)
					{
						// 4-point gaussian interpolation
						d = ch.xsmp_count >> (FIXED_POINT_SHIFT - 8);
						outx  = ((G4(-d) * ch.nb_sample[ ch.nb_index		 ]) >> 11) & ~1;
						outx += ((G3(-d) * ch.nb_sample[(ch.nb_index + 1) & 3]) >> 11) & ~1;
						outx += ((G2( d) * ch.nb_sample[(ch.nb_index + 2) & 3]) >> 11) & ~1;
						outx = ((outx & 0xFFFF) ^ 0x8000) - 0x8000;
						outx += ((G1( d) * ch.nb_sample[(ch.nb_index + 3) & 3]) >> 11) & ~1;
						outx = CLIP16(outx);
					}
					else
					{
						outx = ch.sample;
					}
				}
				else // SAMPLE_NOISE
				{
					noise_count -= noise_rate;
					while (noise_count <= 0)
					{
						noise_count += env_counter_max;
						noise_index = (noise_index + 1) & 0xFF;
					}

					outx = noise_cache[noise_index] >> 16;
				}

				outx = ((outx * ch.xenvx) >> 11) & ~1;
				ch.out_sample = outx;

				if (mod2)
					wave[I] = outx;

				int V;

				V = (outx * ch.volume_left ) >> 7;

				MixBuffer[I] += V;
				ch.echo_buf_ptr[I] += V;
			}

			mono_exit = false;
		}
		
		DoFakeMute = false;
	}

	// For backwards compatibility with older port specific code
	/*
	private void MixSamplesOffset( ShortBuffer buffer, int sample_count, int byte_offset)
	{
		MixSamples(buffer.getOffsetBuffer(byte_offset), sample_count);
	}
	*/

	void MixSamples (ShortArray buffer, int sample_count)
	{	
		int I = 0;
		int J = 0;

		if ( ! so.mute_sound)
		{
			for (int i = 0; i < sample_count; i++)
				MixBuffer[i] = 0;
			
			if ( ! settings.DisableSoundEcho )
			{
				for (int i = 0; i < sample_count; i++)
					EchoBuffer[i] = 0;
			}

			if ( so.stereo )
			{
				MixStereo( sample_count );
			}
			else
			{
				MixMono( sample_count );
			}
		}

		// NAC: 16 bit audio only
		
		/* Mix and convert waveforms 
		if (so.sixteen_bit)
		{
		*/
		
		// 16-bit sound
		if (so.mute_sound)
		{
			buffer.zero();
		}
		else
		{
			if ( ! settings.DisableSoundEcho)
			{
				if( so.stereo )
				{
					// 16-bit stereo sound with echo enabled ...
					if (no_filter)
					{
						// ... but no filter defined.
						for (J = 0; J < sample_count; J++)
						{
							int E = Echo[echo_ptr];

							Loop[FIRIndex & 15] = E;
							E = (E * 127) >> 7;
							FIRIndex++;

							if (echo_write_enabled > 0)
							{
								I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
								I = CLIP16(I);
								Echo[echo_ptr] = I;
							}
							else // FIXME: Sne's echo buffer is not in APU_RAM
								Echo[echo_ptr] = 0;

							if (++echo_ptr >= echo_buffer_size)
								echo_ptr = 0;

							I = (MixBuffer[J] * master_volume[J & 1] +
								E * echo_volume[J & 1]) >> 7;
							I = CLIP16(I);
							
							buffer.put16Bit(J, I);
						}
					}
					else
					{
						// ... with filter defined.
						for (J = 0; J < sample_count; J++)
						{
							int E = Echo[echo_ptr];

							Loop[FIRIndex & 15] = E;
							E  = E * FilterTaps[0];
							E += Loop[(FIRIndex -  2) & 15] * FilterTaps[1];
							E += Loop[(FIRIndex -  4) & 15] * FilterTaps[2];
							E += Loop[(FIRIndex -  6) & 15] * FilterTaps[3];
							E += Loop[(FIRIndex -  8) & 15] * FilterTaps[4];
							E += Loop[(FIRIndex - 10) & 15] * FilterTaps[5];
							E += Loop[(FIRIndex - 12) & 15] * FilterTaps[6];
							E += Loop[(FIRIndex - 14) & 15] * FilterTaps[7];
							E >>= 7;
							FIRIndex++;

							if (echo_write_enabled > 0)
							{
								I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
								I = CLIP16(I);
								Echo[echo_ptr] = I;
							}
							else
							{
								// FIXME: Sne's echo buffer is not in APU_RAM
								Echo[echo_ptr] = 0;
							}

							if (++echo_ptr >= echo_buffer_size)
								echo_ptr = 0;

							I = ( MixBuffer[J] * master_volume[J & 1] + E * echo_volume[J & 1] ) >> 7;
							I = CLIP16(I);
							buffer.put16Bit(J, I);
						}
					}
				}
				else
				{
					// 16-bit mono sound with echo enabled...
					if (no_filter)
					{						
						// ... no filter defined
						for (J = 0; J < sample_count; J++)
						{
							int E = Echo[echo_ptr];

							Loop[FIRIndex & 7] = E;
							E = (E * 127) >> 7;
							FIRIndex++;

							if (echo_write_enabled > 0)
							{
								I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
								I = CLIP16(I);
								Echo[echo_ptr] = I;
							}
							else
							{
								// FIXME: Sne's echo buffer is not in APU_RAM
								Echo[echo_ptr] = 0;
							}
							

							if (++echo_ptr >= echo_buffer_size)
								echo_ptr = 0;

							I = (MixBuffer[J] * master_volume[0] +
								E * echo_volume[0]) >> 7;
							I = CLIP16(I);
							
							buffer.put16Bit(J, I);
							
						}
					}
					else
					{
						// ... with filter defined
						for (J = 0; J < sample_count; J++)
						{
							int E = Echo[echo_ptr];

							Loop[FIRIndex & 7] = E;
							E  = E * FilterTaps[0];
							E += Loop[(FIRIndex - 1) & 7] * FilterTaps[1];
							E += Loop[(FIRIndex - 2) & 7] * FilterTaps[2];
							E += Loop[(FIRIndex - 3) & 7] * FilterTaps[3];
							E += Loop[(FIRIndex - 4) & 7] * FilterTaps[4];
							E += Loop[(FIRIndex - 5) & 7] * FilterTaps[5];
							E += Loop[(FIRIndex - 6) & 7] * FilterTaps[6];
							E += Loop[(FIRIndex - 7) & 7] * FilterTaps[7];
							E >>= 7;
							FIRIndex++;

							if (echo_write_enabled > 0)
							{
								I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
								I = CLIP16(I);
								Echo[echo_ptr] = I;
							}
							else // FIXME: Sne's echo buffer is not in APU_RAM
							{
								Echo[echo_ptr] = 0;
							}

							if (++echo_ptr >= echo_buffer_size)
								echo_ptr = 0;

							I = (MixBuffer[J] * master_volume[0] +
								E * echo_volume[0]) >> 7;
							I = CLIP16(I);
							buffer.put16Bit(J, I);
						}
					}
				}
			}
			else
			{
				// 16-bit mono or stereo sound, no echo
				for (J = 0; J < sample_count; J++)
				{
					I = (MixBuffer[J] * master_volume[J & 1]) >> 7;
					I = CLIP16(I);
					buffer.put16Bit(J, I);
				}
			}
		}
		
		// NAC: Don't expect to do 8-bit sound
		/*
		}
		
		else
		{
			// 8-bit sound
			if (so.mute_sound)
			{
				buffer.fill(128, 0, sample_count);
			}
			else
			{
				if (!Settings.DisableSoundEcho)
				{
					if (so.stereo > 0)
					{
						// 8-bit stereo sound with echo enabled...
						if (no_filter)
						{
							// ... but no filter
							for (J = 0; J < sample_count; J++)
							{
								int E = Echo[echo_ptr];

								Loop[FIRIndex & 15] = E;
								E = (E * 127) >> 7;
								FIRIndex++;

								if (echo_write_enabled > 0)
								{
									I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
									I = CLIP16(I);
									Echo[echo_ptr] = I;
								}
								else // FIXME: Sne's echo buffer is not in APU_RAM
									Echo[echo_ptr] = 0;

								if (++echo_ptr >= echo_buffer_size)
									echo_ptr = 0;

								I = (MixBuffer[J] * master_volume[J & 1] +
									E * echo_volume[J & 1]) >> 15;
								I = CLIP8(I);
								
								buffer.put8Bit(J, I + 128);
							}
						}
						else
						{
							// ... with filter
							for (J = 0; J < sample_count; J++)
							{
								int E = Echo[echo_ptr];

								Loop[FIRIndex & 15] = E;
								E  = E * FilterTaps[0];
								E += Loop[(FIRIndex -  2) & 15] * FilterTaps[1];
								E += Loop[(FIRIndex -  4) & 15] * FilterTaps[2];
								E += Loop[(FIRIndex -  6) & 15] * FilterTaps[3];
								E += Loop[(FIRIndex -  8) & 15] * FilterTaps[4];
								E += Loop[(FIRIndex - 10) & 15] * FilterTaps[5];
								E += Loop[(FIRIndex - 12) & 15] * FilterTaps[6];
								E += Loop[(FIRIndex - 14) & 15] * FilterTaps[7];
								E >>= 7;
								FIRIndex++;

								if (echo_write_enabled > 0)
								{
									I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
									I = CLIP16(I);
									Echo[echo_ptr] = I;
								}
								else // FIXME: Sne's echo buffer is not in APU_RAM
									Echo[echo_ptr] = 0;

								if (++echo_ptr >= echo_buffer_size)
									echo_ptr = 0;

								I = (MixBuffer[J] * master_volume[J & 1] +
									E * echo_volume[J & 1]) >> 15;
								I = CLIP8(I);
								buffer.put8Bit(J, I + 128);
							}
						}
					}
					else
					{
						// 8-bit mono sound with echo enabled...
						if (no_filter)
						{
							// ... but no filter.
							for (J = 0; J < sample_count; J++)
							{
								int E = Echo[echo_ptr];

								Loop[FIRIndex & 7] = E;
								E = (E * 127) >> 7;
								FIRIndex++;

								if (echo_write_enabled > 0)
								{
									I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
									I = CLIP16(I);
									Echo[echo_ptr] = I;
								}
								else // FIXME: Sne's echo buffer is not in APU_RAM
									Echo[echo_ptr] = 0;

								if (++echo_ptr >= echo_buffer_size)
									echo_ptr = 0;

								I = (MixBuffer[J] * master_volume[0] +
									E * echo_volume[0]) >> 15;
								I = CLIP8(I);
								buffer.put8Bit(J, I + 128);
							}
						}
						else
						{
							// ... with filter.
							for (J = 0; J < sample_count; J++)
							{
								int E = Echo[echo_ptr];

								Loop[FIRIndex & 7] = E;
								E  = E * FilterTaps[0];
								E += Loop[(FIRIndex - 1) & 7] * FilterTaps[1];
								E += Loop[(FIRIndex - 2) & 7] * FilterTaps[2];
								E += Loop[(FIRIndex - 3) & 7] * FilterTaps[3];
								E += Loop[(FIRIndex - 4) & 7] * FilterTaps[4];
								E += Loop[(FIRIndex - 5) & 7] * FilterTaps[5];
								E += Loop[(FIRIndex - 6) & 7] * FilterTaps[6];
								E += Loop[(FIRIndex - 7) & 7] * FilterTaps[7];
								E >>= 7;
								FIRIndex++;

								if (echo_write_enabled > 0)
								{
									I = EchoBuffer[J] + ((E * echo_feedback) >> 7);
									I = CLIP16(I);
									Echo[echo_ptr] = I;
								}
								else // FIXME: Sne's echo buffer is not in APU_RAM
									Echo[echo_ptr] = 0;

								if (++echo_ptr >= echo_buffer_size)
									echo_ptr = 0;

								I = (MixBuffer[J] * master_volume[0] +
									E * echo_volume[0]) >> 15;
								I = CLIP8(I);
								buffer.put8Bit(J, I + 128);
							}
						}
					}
				}
				else
				{
					// 8-bit mono or stereo sound, no echo
					for (J = 0; J < sample_count; J++)
					{
						I = (MixBuffer[J] * master_volume[J & 1]) >> 15;
						I = CLIP8(I);
						buffer.put8Bit(J, I + 128);
					}
				}
			}
		}
		*/
	}

	void ResetSound (boolean full)
	{	
		for (int i = 0; i < NUM_CHANNELS; i++)
		{
			AudioChannel channel = new AudioChannel();
			channel.state = SOUND_SILENT;
			channel.mode = MODE_NONE;
			channel.type = SOUND_SAMPLE;
			channel.volume_left = 0;
			channel.volume_right = 0;
			channel.hertz = 0;
			channel.loop = false;
			channel.xsmp_count = 0;
			channel.xenvx = 0;
			channel.xenvx_target = 0;
			channel.xenv_count = 0;
			channel.xenv_rate = 0;
			channel.xattack_rate = 0;
			channel.xdecay_rate = 0;
			channel.xsustain_rate = 0;
			channel.xsustain_level = 0;
			
			if( full )
			{
				channel.out_sample = 0;
				channel.block_pointer = 0;
				channel.sample_pointer = 0;
				channel.sample = 0;
				channel.sample_number = 0;
				channel.last_block = false;
				for(int j = 0 ; j < 2 ; j++) channel.previous[j] = 0;
				for(int j = 0 ; j < 2 ; j++) channel.previous16[j] = 0;
				for(int j = 0 ; j < 16 ; j++) channel.decoded[j] = 0;
			}
			
			channels[i] = channel;
		}

		FilterTaps[0] = 127;
		FilterTaps[1] = 0;
		FilterTaps[2] = 0;
		FilterTaps[3] = 0;
		FilterTaps[4] = 0;
		FilterTaps[5] = 0;
		FilterTaps[6] = 0;
		FilterTaps[7] = 0;

		rand_seed = 1;

		so.mute_sound = true;
		so.noise_gen = 1;
		so.sound_switch = 255;
		so.stereo_switch = ~0;
		so.samples_mixed_so_far = 0;
		so.play_position = 0;
		so.err_counter = 0;

		if (full)
		{
			echo_volume_left = 0;
			echo_volume_right = 0;

			echo_write_enabled = 0;
			pitch_mod = 0;

			echo_volume[0] = 0;
			echo_volume[1] = 0;
			noise_rate = 0;
			
			for( int i = 0; i < Loop.length; i++)
				Loop[i] = 0;

			for( int i = 0; i < Echo.length; i++)
				Echo[i] = 0;
		}

		// At least Super Bomberman 2 requires the defaule master volume is not zero.
		master_volume_left  = 127;
		master_volume_right = 127;
		master_volume [0] = master_volume [1] = 127;
		no_filter = true;
		echo_ptr = 0;
		echo_feedback = 0;
		echo_buffer_size = 1;

		if (so.playback_rate > 0)
			so.err_rate = (int) (FIXED_POINT * SnesSystem.SNES_SCANLINE_TIME * so.playback_rate);
		else
			so.err_rate = 0;
	}

	void SetPlaybackRate( int playback_rate )
	{
		if (playback_rate > 48000)
			playback_rate = 48000;

		so.playback_rate = playback_rate;
		so.err_rate = (int) (FIXED_POINT * SnesSystem.SNES_SCANLINE_TIME * so.playback_rate);

		
		for( int i = 0; i < Loop.length; i++)
			Loop[i] = 0;

		for( int i = 0; i < Echo.length; i++)
			Echo[i] = 0;
		
		SetEchoDelay (apu.DSP[APU.APU_EDL] & 0xF);

		for (int i = 0; i < NUM_CHANNELS; i++)
			SetSoundFrequency (i, channels[i].hertz);

		env_counter_max = env_counter_max_master * playback_rate / 32000;
	}

	boolean InitSound (int mode, boolean stereo, int buffer_size)
	{
		so.sound_fd = -1;
		so.sound_switch = 255;
		so.stereo_switch = ~0;

		so.playback_rate = 0;
		so.buffer_size = 0;
		so.stereo = stereo;
		so.sixteen_bit = settings.SixteenBitSound;
		so.encoded = false;
		so.pitch_mul = 0.985; // XXX: necessary for most cards in linux...?

		ResetSound( true );

		if ( (mode & 7) == 0)
			return true;

		SetSoundMute(true);
		
		if ( ! SnesSystem.OpenSoundDevice(mode, stereo, buffer_size) )
		{
			//NAC: Disabled for compares
			// SSystem.Message (SSystem._ERROR, SSystem._SOUND_DEVICE_OPEN_FAILED, "Sound device open failed");
			return false;
		}

		return true;
	}

	boolean SetSoundMode(int channel, int mode)
	{
		AudioChannel ch = channels[channel];

		switch (mode)
		{
			case MODE_RELEASE:
				if (ch.mode != MODE_NONE)
				{
					ch.mode = MODE_RELEASE;
					return (true);
				}
				break;

			case MODE_DECREASE_LINEAR:
			case MODE_DECREASE_EXPONENTIAL:
			case MODE_GAIN:
				if (ch.mode != MODE_RELEASE)
				{
					ch.mode = mode;
					if (ch.state != SOUND_SILENT)
						ch.state = mode;

					return (true);
				}
				break;

			case MODE_INCREASE_LINEAR:
			case MODE_INCREASE_BENT_LINE:
				if (ch.mode != MODE_RELEASE)
				{
					ch.mode = mode;
					if (ch.state != SOUND_SILENT)
						ch.state = mode;

					return (true);
				}
				break;

			case MODE_ADSR:
				if (ch.mode == MODE_NONE || ch.mode == MODE_ADSR)
				{
					ch.mode = mode;
					return (true);
				}
		}

		return (false);
	}

	private void SetSoundControl (int sound_switch)
	{
		so.sound_switch = sound_switch;
	}

	void PlaySample (int channel)
	{
		AudioChannel ch = channels[channel];

		ch.state = SOUND_SILENT;
		ch.mode = MODE_NONE;
		ch.xenvx = 0;

		apu.FixEnvelope( channel,
			apu.DSP[APU.APU_GAIN  + (channel << 4)],
			apu.DSP[APU.APU_ADSR1 + (channel << 4)],
			apu.DSP[APU.APU_ADSR2 + (channel << 4)]);

		ch.sample_number = apu.DSP[APU.APU_SRCN + channel * 0x10];
		if ( ( apu.DSP[APU.APU_NON] & (1 << channel) ) > 0)
			ch.type = SOUND_NOISE;
		else
			ch.type = SOUND_SAMPLE;

		SetSoundFrequency (channel, ch.hertz);
		ch.loop = false;
		ch.needs_decode = true;
		ch.last_block = false;
		ch.previous[0] = ch.previous[1] = 0;
		ByteArrayOffset dir = apu.GetSampleAddress( ch.sample_number );
		ch.block_pointer = dir.get16Bit(0);
		ch.sample_pointer = 0;
		ch.xenv_count = env_counter_max;
		ch.xsmp_count = 3 * FIXED_POINT; // since gaussian interpolation uses 4 points
		ch.nb_sample[0] = 0;
		ch.nb_sample[1] = 0;
		ch.nb_sample[2] = 0;
		ch.nb_sample[3] = 0;
		ch.nb_index = 0;

		switch (ch.mode)
		{
			case MODE_ADSR: // FIXME: rapid attack
				if (ch.xattack_rate == env_counter_max_master)
				{
					ch.xenvx = ENV_MAX;
					if (ch.xsustain_level == ENV_RANGE)
					{
						ch.state = SOUND_SUSTAIN;
						SetEnvRate (ch, ch.xsustain_rate, 0);
					}
					else
					{
						ch.state = SOUND_DECAY;
						SetEnvRate (ch, ch.xdecay_rate, ch.xsustain_level);
					}
				}
				else
				{
					ch.state = SOUND_ATTACK;
					ch.xenvx = 0;
					SetEnvRate (ch, ch.xattack_rate, ENV_MAX);
				}

				break;

			case MODE_GAIN:
				ch.state = SOUND_GAIN;
				break;

			case MODE_INCREASE_LINEAR:
				ch.state = SOUND_INCREASE_LINEAR;
				break;

			case MODE_INCREASE_BENT_LINE:
				ch.state = SOUND_INCREASE_BENT_LINE;
				break;

			case MODE_DECREASE_LINEAR:
				ch.state = SOUND_DECREASE_LINEAR;
				break;

			case MODE_DECREASE_EXPONENTIAL:
				ch.state = SOUND_DECREASE_EXPONENTIAL;
				break;

			default:
				break;
		}

		apu.FixEnvelope(channel,
			apu.DSP[APU.APU_GAIN  + (channel << 4)],
			apu.DSP[APU.APU_ADSR1 + (channel << 4)],
			apu.DSP[APU.APU_ADSR2 + (channel << 4)]);
	}
	
	private final int CLIP16( int v)
	{
		if ( v < -32768 )
			return -32768; 
		else if ( v > 32767)
			return 32767;
		
		return v;
	}
}