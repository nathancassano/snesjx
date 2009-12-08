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

import java.net.*;

class NetPlayServer
{
	public static final int NP_MAX_CLIENTS = 8;
	public NetPlayClient Clients[] = new NetPlayClient[NP_MAX_CLIENTS];
	public int NumClients;
	public NetPlayServerTask TaskQueue[] = new NetPlayServerTask[NetPlayServerTask.NP_MAX_TASKS];
	public int TaskHead;
	public int TaskTail;
	public ServerSocket Socket;
	public int FrameTime;
	public int FrameCount;
	public String ROMName;
	public int Joypads[] = new int[NP_MAX_CLIENTS];
	public boolean  ClientPaused;
	public int Paused;
	public boolean  SendROMImageOnConnect;
	public boolean  SyncByReset;

	public static final int NP_SERV_HELLO = 0;
	public static final int NP_SERV_JOYPAD = 1; 
	public static final int NP_SERV_RESET = 2;
	public static final int NP_SERV_PAUSE = 3;
	public static final int NP_SERV_LOAD_ROM = 4;
	public static final int NP_SERV_ROM_IMAGE = 5;
	public static final int NP_SERV_FREEZE_FILE = 6;
	public static final int NP_SERV_SRAM_DATA = 7;
	public static final int NP_SERV_READY = 8;
	
	public static final int NP_SERVER_SEND_ROM_IMAGE = 0;
	public static final int NP_SERVER_SYNC_ALL = 1;
	public static final int NP_SERVER_SYNC_CLIENT = 2;
	public static final int NP_SERVER_SEND_FREEZE_FILE_ALL = 3;
	public static final int NP_SERVER_SEND_ROM_LOAD_REQUEST_ALL = 4;
	public static final int NP_SERVER_RESET_ALL = 5;
	public static final int NP_SERVER_SEND_SRAM_ALL = 6;
	public static final int NP_SERVER_SEND_SRAM = 7;
	
	private Globals globals;
	private Settings settings;

	public class NetPlayClient
	{
		public int SendSequenceNum;
		public int ReceiveSequenceNum;
		public boolean Connected;
		public boolean SaidHello;
		public boolean Paused;
		public boolean Ready;
		public Socket Socket;
		public String ROMName;
		public String HostName;
		public String Who;
	}
	
	public class NetPlayServerTask
	{
		public static final int NP_MAX_TASKS = 20;
		
		public int Task;
		public ByteArrayOffset Data;
		
	};
	
	public void setUp()
	{

	}
	/*
	public void NPShutdownClient (int c )
	{
		NPShutdownClient(c, false);
	}
	
	public void NPShutdownClient (int c, boolean report_error )
	{
		if (Clients[c].Connected)
		{
			Clients[c].Connected = false;
			Clients[c].SaidHello = false;

			Clients[c].Socket.close();

			if (report_error)
			{
				System.out.printf("Player %d on '%s' has disconnected.", c + 1, Clients[c].HostName);
				NPSetWarning(NetPlay.ErrorMsg);
			}

			if (Clients[c].HostName.length() > 0 )
			{
				Clients[c].HostName = null;
			}
			if (Clients[c].ROMName.length() > 0 )
			{
				Clients[c].ROMName = null;
			}
			if (Clients[c].Who.length() > 0 )
			{
				Clients[c].Who = null;
			}
			Joypads [c] = 0;
			NumClients--;
			NPRecomputePause ();
		}
	}

	public boolean NPSGetData (Socket socket, ByteBufferOffset data, int length)
	{
		int len = length;
		int ptr;

		do
		{
			int num_bytes = len;

			// Read the data in small chunks, allowing this thread to spot an
			// abort request from another thread.
			if (num_bytes > 512)
				num_bytes = 512;

			int got = read( socket, (char *) ptr, num_bytes);
			
			if (got < 0)
			{
				if (errno == EINTR )
					continue;

				return (false);
			}
			else
				if (got == 0)
					return (false);

			len -= got;
			ptr += got;
		} while (len > 0);

		return (true);
	}

	public boolean NPSSendData (int fd, ByteBufferOffset data, int length)
	{
		int Percent = 0;
		int len = length;
		int chunk = length / 50;

		if (chunk < 1024)
			chunk = 1024;

		do
		{
			int num_bytes = len;

			// Write the data in small chunks, allowing this thread to spot an
			// abort request from another thread.
			if (num_bytes > chunk)
				num_bytes = chunk;

		int sent;
		sent = write (fd, (char *) data, len);

		if (sent < 0)
		{
			if (errno == EINTR)
			{
				continue;
			}
			return (false);
		}
		else
		if (sent == 0)
			return (false);
		len -= sent;
		data += sent;
			if (length > 1024)
			{
				Percent = (int) (((length - len) * 100) / length);

			}
		} while (len > 0);

		return (true);
	}

	public void NPSendHeartBeat ()
	{
		int len = 3;
		int data [3 + 4 * 5];
		int *ptr = data;
		int n;

		for (n = NetPlay.NP_MAX_CLIENTS - 1; n >= 0; n--)
		{
			if (Clients[n].SaidHello)
				break;
		}

		if (n >= 0)
		{
			boolean Paused = Paused != 0;

			FrameCount++;
			*ptr++ = NP_SERV_MAGIC;
			*ptr++ = 0; // Individual client sequence number will get placed here
			*ptr++ = NP_SERV_JOYPAD | (n << 6) | ((Paused != 0) << 5);

			WRITE_LONG (ptr, FrameCount);
			len += 4;
			ptr += 4;

			int i;

			for (i = 0; i <= n; i++)
			{
				WRITE_LONG (ptr, Joypads [i]);
				len += 4;
				ptr += 4;
			}

			NPSendToAllClients (data, len);
		}
	}

	public void NPSendToAllClients (int *data, int len)
	{
		int i;

		for (i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
		if (Clients[i].SaidHello)
		{
				data [1] = Clients[i].SendSequenceNum++;
			if (!NPSSendData (Clients[i].Socket, data, len))
			NPShutdownClient (i, true);
		}
		}
	}

	public void NPProcessClient (int c)
	{
		int header [7];
		int *data;
		int len;
		int *ptr;

		if (!NPSGetData (Clients[c].Socket, header, 7))
		{
			NPSetWarning ("SERVER: Failed to get message header from client.\n");
			NPShutdownClient (c, true);
			return;
		}
		if (header [0] != NP_CLNT_MAGIC)
		{
			NPSetWarning ("SERVER: Bad header magic value received from client.\n");
			NPShutdownClient (c, true);
			return;
		}

		if (header [1] != Clients[c].ReceiveSequenceNum)
		{
			sprintf (NetPlay.WarningMsg,
					 "SERVER: Messages lost from '%s', expected %d, got %d\n",
					Clients[c].HostName ?
					Clients[c].HostName : "Unknown",
					Clients[c].ReceiveSequenceNum,
					header [1]);
			Clients[c].ReceiveSequenceNum = header [1] + 1;
			NPSetWarning (NetPlay.WarningMsg);
		}
		else
			Clients[c].ReceiveSequenceNum++;

		len = READ_LONG (&header [3]);

		switch (header [2] & 0x3f)
		{
			case NP_CLNT_HELLO:

				NPSetAction ("Got HELLO from client...", true);
				if (len > 0x10000)
				{
					NPSetWarning ("SERVER: Client HELLO message length error.");
					NPShutdownClient (c, true);
					return;
				}
				data = new int [len - 7];
				if (!NPSGetData (Clients[c].Socket, data, len - 7))
				{
					NPSetWarning ("SERVER: Failed to get HELLO message content from client.");
					NPShutdownClient (c, true);
					return;
				}

				if (NumClients <= NP_ONE_CLIENT)
				{
			FrameTime = READ_LONG (data);
			strncpy (ROMName, (char *) &data [4], 29);
			ROMName [29] = 0;
				}

				Clients[c].ROMName = strdup ((char *) &data [4]);

				Clients[c].SendSequenceNum = 0;

				len = 7 + 1 + 1 + 4 + strlen (ROMName) + 1;

				delete data;
				ptr = data = new int [len];
				*ptr++ = NP_SERV_MAGIC;
				*ptr++ = Clients[c].SendSequenceNum++;

				if (SendROMImageOnConnect &&
					NumClients > NP_ONE_CLIENT)
					*ptr++ = NP_SERV_HELLO | 0x80;
				else
					*ptr++ = NP_SERV_HELLO;
				WRITE_LONG (ptr, len);
				ptr += 4;
				*ptr++ = NP_VERSION;
				*ptr++ = c + 1;
				WRITE_LONG (ptr, FrameCount);
				ptr += 4;
				strcpy ((char *) ptr, ROMName);

				NPSetAction ("SERVER: Sending welcome information to new client...", true);
				if (!NPSSendData (Clients[c].Socket, data, len))
				{
					NPSetWarning ("SERVER: Failed to send welcome message to client.");
					NPShutdownClient (c, true);
					return;
				}
				delete data;

				NPSetAction ("SERVER: Waiting for a response from the client...", true);
				break;

			case NP_CLNT_LOADED_ROM:

				Clients[c].SaidHello = true;
				Clients[c].Ready = false;
				Clients[c].Paused = false;
				NPRecomputePause ();
				NPWaitForEmulationToComplete ();

				if (SyncByReset)
				{
					NPServerAddTask (NP_SERVER_SEND_SRAM, (void *) c);
					NPServerAddTask (NP_SERVER_RESET_ALL, 0);
				}
				else
					NPServerAddTask (NP_SERVER_SYNC_CLIENT, (void *) c);
				break;

			case NP_CLNT_RECEIVED_ROM_IMAGE:

				Clients[c].SaidHello = true;
				Clients[c].Ready = false;
				Clients[c].Paused = false;
				NPRecomputePause ();
				NPWaitForEmulationToComplete ();

				if (SyncByReset)
				{
					NPServerAddTask (NP_SERVER_SEND_SRAM, (void *) c);
					NPServerAddTask (NP_SERVER_RESET_ALL, 0);
				}
				else
					NPServerAddTask (NP_SERVER_SYNC_CLIENT, (void *) c);

				break;

			case NP_CLNT_WAITING_FOR_ROM_IMAGE:

				Clients[c].SaidHello = true;
				Clients[c].Ready = false;
				Clients[c].Paused = false;
				NPRecomputePause ();
				NPSendROMImageToClient (c);
				break;

			case NP_CLNT_READY:

				if (Clients[c].SaidHello)
				{
					Clients[c].Paused = false;
					Clients[c].Ready = true;

					NPRecomputePause ();
					break;
				}
				Clients[c].SaidHello = true;
				Clients[c].Ready = true;
				Clients[c].Paused = false;
				NPRecomputePause ();


				if (NumClients > NP_ONE_CLIENT)
				{
					if (!SendROMImageOnConnect)
					{
						NPWaitForEmulationToComplete ();

						if (SyncByReset)
						{
							NPServerAddTask (NP_SERVER_SEND_SRAM, (void *) c);
							NPServerAddTask (NP_SERVER_RESET_ALL, 0);
						}
						else

							// We need to resync all clients on new player connect as we don't have a 'reference game' 
							NPServerAddTask (NP_SERVER_SYNC_ALL, (void *) c);
					}
				}
				else
				{
					Clients[c].Ready = true;
					NPRecomputePause ();
				}
				break;
			case NP_CLNT_JOYPAD:
				Joypads [c] = len;
				break;
			case NP_CLNT_PAUSE:

				Clients[c].Paused = (header [2] & 0x80) != 0;
				if (Clients[c].Paused)
					sprintf (NetPlay.WarningMsg, "SERVER: Client %d has paused.", c + 1);
				else
					sprintf (NetPlay.WarningMsg, "SERVER: Client %d has resumed.", c + 1);
				NPSetWarning (NetPlay.WarningMsg);
				NPRecomputePause ();
				break;
		}
	}

	public void NPAcceptClient (int Listen, boolean block)
	{
		struct sockaddr_in remote_address;
		struct linger val2;
		struct hostent *host;
		int new_fd;
		int i;

		NPSetAction ("SERVER: Attempting to accept client connection...", true);
		memset (&remote_address, 0, sizeof (remote_address));
		ACCEPT_SIZE_T len = sizeof (remote_address);

		new_fd = accept (Listen, (struct sockaddr *)&remote_address, &len);

		NPSetAction ("Setting socket options...", true);
		val2.l_onoff = 1;
		val2.l_linger = 0;
		if (setsockopt (new_fd, SOL_SOCKET, SO_LINGER,
				(char *) &val2, sizeof (val2)) < 0)
		{
			NPSetError ("Setting socket options failed.");
		close (new_fd);
			return;
		}

		for (i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
		if (!Clients[i].Connected)
		{
				NumClients++;
			Clients[i].Socket = new_fd;
				Clients[i].SendSequenceNum = 0;
				Clients[i].ReceiveSequenceNum = 0;
				Clients[i].Connected = true;
				Clients[i].SaidHello = false;
				Clients[i].Paused = false;
				Clients[i].Ready = false;
				Clients[i].ROMName = NULL;
				Clients[i].HostName = NULL;
				Clients[i].Who = NULL;
			break;
		}
		}

		if (i >= NetPlay.NP_MAX_CLIENTS)
		{
			NPSetError ("SERVER: Maximum number of NetPlay Clients have already connected.");
		close (new_fd);
		return;
		}

		if (remote_address.sin_family == AF_INET)
		{

			NPSetAction ("SERVER: Looking up new client's hostname...", true);
		host = gethostbyaddr ((char *) &remote_address.sin_addr,
					  sizeof (remote_address.sin_addr), AF_INET);

		if (host)
		{

			sprintf (NetPlay.WarningMsg, "SERVER: Player %d on %s has connected.", i + 1, host->h_name);
			Clients[i].HostName = strdup (host->h_name);
		}
			else
			{
				char *ip = inet_ntoa (remote_address.sin_addr);
				if (ip)
					Clients[i].HostName = strdup (ip);

			sprintf (NetPlay.WarningMsg, "SERVER: Player %d on %s has connected.", i + 1, ip ? ip : "Unknown");
			}
			NPSetWarning (NetPlay.WarningMsg);
		}

		NPSetAction ("SERVER: Waiting for HELLO message from new client...");
	}

	static boolean server_continue = true;

	static boolean NPServerInit (int port)
	{
		struct sockaddr_in address;
		int i;
		int val;

		if (!NPInitialise ())
			return (false);

		for (i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
			Clients[i].SendSequenceNum = 0;
			Clients[i].ReceiveSequenceNum = 0;
			Clients[i].Connected = false;
			Clients[i].SaidHello = false;
			Clients[i].Paused = false;
			Clients[i].Ready = false;
			Clients[i].Socket = 0;
			Clients[i].ROMName = NULL;
			Clients[i].HostName = NULL;
			Clients[i].Who = NULL;
			Joypads [i] = 0;
		}

		NumClients = 0;
		FrameCount = 0;

		if ((Socket = socket (AF_INET, SOCK_STREAM, 0)) < 0)
		{
		NPSetError ("NetPlay Server: Can't create listening socket.");
		return (false);
		}

		val = 1;
		setsockopt (Socket, SOL_SOCKET, SO_REUSEADDR,
					(char *)&val, sizeof (val));

		memset (&address, 0, sizeof (address));
		address.sin_family = AF_INET;
		address.sin_addr.s_addr = htonl (INADDR_ANY);
		address.sin_port = htons (port);

		if (bind (Socket, (struct sockaddr *) &address, sizeof (address)) < 0)
		{
			NPSetError ("NetPlay Server: Can't bind socket to port number.\nPort already in use?");
			return (false);
		}

		if (listen (Socket, NetPlay.NP_MAX_CLIENTS) < 0)
		{
			NPSetError ("NetPlay Server: Can't get new socket to listen.");
			return (false);
		}

		return (true);
	}

	public void NPSendServerPause (boolean paused)
	{

		int pause = new int[7];
		int *ptr = pause;
		*ptr++ = NP_SERV_MAGIC;
		*ptr++ = 0;
		*ptr++ = NP_SERV_PAUSE | (paused ? 0x20 : 0);
		WRITE_LONG (ptr, FrameCount);
		NPSendToAllClients (pause, 7);
	}

	public void NPServerLoop (void *)
	{

		boolean success = false;
		static struct timeval next1 = {0, 0};
		struct timeval now;

		int pausedState = -1, newPausedState = -1;

		while (server_continue)
		{
			fd_set read_fds;
			struct timeval timeout;
			int res;
			int i;

			int max_fd = Socket;

			if (success && !(Settings.Paused && !Settings.FrameAdvance) && !Settings.StopEmulation &&
				!Settings.ForcedPause && !Paused)
			{
				NPSendHeartBeat ();
				newPausedState = 0;
			}
			else
			{
				newPausedState = 1;
			}

			if(pausedState != newPausedState)
			{
				pausedState = newPausedState;
//				NPSendServerPause(pausedState); // XXX: doesn't seem to work yet...
			}

			do
			{
				FD_ZERO (&read_fds);
				FD_SET (Socket, &read_fds);
				for (i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
				{
					if (Clients[i].Connected)
					{
						FD_SET (Clients[i].Socket, &read_fds);
						if (Clients[i].Socket > max_fd)
							max_fd = Clients[i].Socket;
					}
				}

				timeout.tv_sec = 0;
				timeout.tv_usec = 1000;
				res = select (max_fd + 1, &read_fds, NULL, NULL, &timeout);

				if (res > 0)
				{
					if (FD_ISSET (Socket, &read_fds))
						NPAcceptClient (Socket, false);

					for (i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
					{
						if (Clients[i].Connected &&
							FD_ISSET (Clients[i].Socket, &read_fds))
						{
							NPProcessClient (i);
						}
					}
				}
			} while (res > 0);

			while (gettimeofday (&now, NULL) < 0) ;

			// If there is no known "next" frame, initialize it now 
			if (next1.tv_sec == 0) { next1 = now; ++next1.tv_usec; }

		success=false;

		if (timercmp(&next1, &now, >))
			{
				// If we're ahead of time, sleep a while 
				unsigned timeleft =
					(next1.tv_sec - now.tv_sec) * 1000000
					+ next1.tv_usec - now.tv_usec;
			usleep(timeleft<(200*1000)?timeleft:(200*1000));
			}

			if (!timercmp(&next1, &now, >))
			{

				// Calculate the timestamp of the next frame.
				next1.tv_usec += Settings.FrameTime;
				if (next1.tv_usec >= 1000000)
				{
					next1.tv_sec += next1.tv_usec / 1000000;
					next1.tv_usec %= 1000000;
				}
				success=true;
			 }

			while (TaskHead != TaskTail)
			{
				void *task_data = TaskQueue [TaskHead].Data;

				switch (TaskQueue [TaskHead].Task)
				{
					case NP_SERVER_SEND_ROM_IMAGE:
						NPSendROMImageToAllClients ();
						break;
					case NP_SERVER_SYNC_CLIENT:
						Clients[(pint) task_data].Ready = false;
						NPRecomputePause ();
						NPSyncClient ((pint) task_data);
						break;
					case NP_SERVER_SYNC_ALL:
						NPSyncClients ();
						break;
					case NP_SERVER_SEND_FREEZE_FILE_ALL:
						NPSendFreezeFileToAllClients ((char *) task_data);
						free ((char *) task_data);
						break;
					case NP_SERVER_SEND_ROM_LOAD_REQUEST_ALL:
						NPSendROMLoadRequest ((char *) task_data);
						free ((char *) task_data);
						break;
					case NP_SERVER_RESET_ALL:
						NPNoClientReady (0);
						NPWaitForEmulationToComplete ();
						NPSetAction ("SERVER: Sending RESET to all clients...", true);
						{
							int reset [7];
							int *ptr;

							ptr = reset;
							*ptr++ = NP_SERV_MAGIC;
							*ptr++ = 0;
							*ptr++ = NP_SERV_RESET;
							WRITE_LONG (ptr, FrameCount);
							NPSendToAllClients (reset, 7);
						}
						NPSetAction ("", true);
						break;
					case NP_SERVER_SEND_SRAM:
						Clients[(pint) task_data].Ready = false;
						NPRecomputePause ();
						NPWaitForEmulationToComplete ();
						NPSendSRAMToClient ((pint) task_data);
						break;

					case NP_SERVER_SEND_SRAM_ALL:
						NPNoClientReady ();
						NPWaitForEmulationToComplete ();
						NPSendSRAMToAllClients ();
						break;

					default:
						NPSetError ("SERVER: *** Unknown task ***\n");
						break;
				}
				TaskHead = (TaskHead + 1) % NP_MAX_TASKS;
			}
		}

		NPStopServer ();
	}

	boolean NPStartServer (int port)
	{
		static int p;

		p = port;
		server_continue = true;
		if (NPServerInit (port))
			NPServerLoop(NULL);
		return (true);

		return (false);
	}

	public void NPStopServer()
	{
		server_continue = false;
		close (Socket);

		for (int i = 0; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
			if (Clients[i].Connected)
			NPShutdownClient(i, false);
		}
	}

	public void NPSendROMImageToAllClients()
	{
		NPNoClientReady ();
		NPWaitForEmulationToComplete ();

		int c;

		for (c = NP_ONE_CLIENT; c < NetPlay.NP_MAX_CLIENTS; c++)
		{
			if (Clients[c].SaidHello)
				NPSendROMImageToClient (c);
		}

		if (SyncByReset)
		{
			NPServerAddTask (NP_SERVER_SEND_SRAM_ALL, 0);
			NPServerAddTask (NP_SERVER_RESET_ALL, 0);
		}
		else
			NPSyncClient (-1);
	}

	boolean NPSendROMImageToClient (int c)
	{
		sprintf (NetPlay.ActionMsg, "Sending ROM image to player %d...", c + 1);
		NPSetAction (NetPlay.ActionMsg, true);

		int header [7 + 1 + 4];
		int *ptr = header;
		int len = sizeof (header) + Memory.CalculatedSize +
				  strlen (Memory.ROMFilename) + 1;
		*ptr++ = NP_SERV_MAGIC;
		*ptr++ = Clients[c].SendSequenceNum++;
		*ptr++ = NP_SERV_ROM_IMAGE;
		WRITE_LONG (ptr, len);
		ptr += 4;
		*ptr++ = Memory.HiROM;
		WRITE_LONG (ptr, Memory.CalculatedSize);

		if (!NPSSendData (Clients[c].Socket, header, sizeof (header)) ||
			!NPSSendData (Clients[c].Socket, Memory.ROM,
							Memory.CalculatedSize) ||
			!NPSSendData (Clients[c].Socket, (int *) Memory.ROMFilename,
							strlen (Memory.ROMFilename) + 1))
		{
			NPShutdownClient (c, true);
			return (false);
		}
		return (true);
	}

	public void NPSyncClients ()
	{
		NPNoClientReady ();
		NPSyncClient (-1);
	}

	public void NPSyncClient (int client)
	{
		char fname[] = "/tmp/sne_fztmpXXXXXX";
		int fd=-1;

		NPWaitForEmulationToComplete ();

		NPSetAction ("SERVER: Freezing game...", true);

		if ( ((fd=mkstemp(fname)) >= 0) && FreezeGame(fname) )
		{
			int *data;
			int len;

			NPSetAction ("SERVER: Loading freeze file...", true);
			if (NPLoadFreezeFile (fname, data, len))
			{
				int c;

				if (client < 0)
				{
					for (c = NP_ONE_CLIENT; c < NetPlay.NP_MAX_CLIENTS; c++)
					{
						if (Clients[c].SaidHello)
						{
							Clients[client].Ready = false;
							NPRecomputePause ();
							NPSendFreezeFile (c, data, len);
						}
					}
				}
				else
				{
					Clients[client].Ready = false;
					NPRecomputePause ();
					NPSendFreezeFile (client, data, len);
				}
				delete data;
			}
			remove (fname);
		}

		if (fd != -1)
			close(fd);
	}

	boolean NPLoadFreezeFile (String fname, int *&data, int &len)
	{
		FILE *ff;

		if ((ff = fopen (fname, "rb")))
		{
			fseek (ff, 0, SEEK_END);
			len = ftell (ff);
			fseek (ff, 0, SEEK_SET);

			data = new int [len];
			boolean ok = (fread (data, 1, len, ff) == len);
			fclose (ff);

			return (ok);
		}
		return (false);
	}

	public void NPSendFreezeFile (int c, int *data, int len)
	{
		sprintf (NetPlay.ActionMsg, "SERVER: Sending freeze-file to player %d...", c + 1);
		NPSetAction (NetPlay.ActionMsg, true);
		int header [7 + 4];
		int *ptr = header;

		*ptr++ = NP_SERV_MAGIC;
		*ptr++ = Clients[c].SendSequenceNum++;
		*ptr++ = NP_SERV_FREEZE_FILE;
		WRITE_LONG (ptr, len + 7 + 4);
		ptr += 4;
		WRITE_LONG (ptr, FrameCount);

		if (!NPSSendData (Clients[c].Socket, header, 7 + 4) ||
			!NPSSendData (Clients[c].Socket, data, len))
		{
		   NPShutdownClient (c, true);
		}
		NPSetAction ("", true);
	}

	public void NPRecomputePause ()
	{
		int c;

		for (c = 0; c < NetPlay.NP_MAX_CLIENTS; c++)
		{
			if (Clients[c].SaidHello &&
				(!Clients[c].Ready || Clients[c].Paused))
			{
				Paused = true;
				return;
			}
		}
		Paused = false;
	}

	public void NPNoClientReady (int start_index)
	{
		int c;

		for (c = start_index; c < NetPlay.NP_MAX_CLIENTS; c++)
			Clients[c].Ready = false;
		NPRecomputePause ();
	}

	public void NPSendROMLoadRequest (String filename)
	{
		NPNoClientReady ();

		int len = 7 + strlen (filename) + 1;
		int *data = new int [len];
		int *ptr = data;
		*ptr++ = NP_SERV_MAGIC;
		*ptr++ = 0;
		*ptr++ = NP_SERV_LOAD_ROM;
		WRITE_LONG (ptr, len);
		ptr += 4;
		strcpy ((char *) ptr, filename);

		for (int i = NP_ONE_CLIENT; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
		if (Clients[i].SaidHello)
		{
				sprintf (NetPlay.WarningMsg, "SERVER: sending ROM load request to player %d...", i + 1);
				NPSetAction (NetPlay.WarningMsg, true);
				data [1] = Clients[i].SendSequenceNum++;
			if (!NPSSendData (Clients[i].Socket, data, len))
				{
			NPShutdownClient (i, true);
				}
			}
		}
		delete data;
	}

	public void NPSendSRAMToAllClients ()
	{
		int i;

		for (i = NP_ONE_CLIENT; i < NetPlay.NP_MAX_CLIENTS; i++)
		{
			if (Clients[i].SaidHello)
				NPSendSRAMToClient (i);
		}
	}

	public void NPSendSRAMToClient (int c)
	{
		int sram [7];
		int SRAMSize = Memory.SRAMSize ?
					   (1 << (Memory.SRAMSize + 3)) * 128 : 0;
		if (SRAMSize > 0x10000)
			SRAMSize = 0x10000;
		int len = 7 + SRAMSize;

		sprintf (NetPlay.ActionMsg, "SERVER: Sending S-RAM to player %d...", c + 1);
		NPSetAction (NetPlay.ActionMsg, true);

		int *ptr = sram;
		*ptr++ = NP_SERV_MAGIC;
		*ptr++ = Clients[c].SendSequenceNum++;
		*ptr++ = NP_SERV_SRAM_DATA;
		WRITE_LONG (ptr, len);
		if (!NPSSendData (Clients[c].Socket,
							sram, sizeof (sram)) ||
			(len > 7 &&
			 !NPSSendData (Clients[c].Socket,
							 Memory.SRAM, len - 7)))
		{
			NPShutdownClient (c, true);
		}
	}

	public void NPSendFreezeFileToAllClients (String filename)
	{
		int *data;
		int len;

		if (NumClients > NP_ONE_CLIENT && NPLoadFreezeFile (filename, data, len))
		{
			NPNoClientReady ();

			for (int c = NP_ONE_CLIENT; c < NetPlay.NP_MAX_CLIENTS; c++)
			{
				if (Clients[c].SaidHello)
					NPSendFreezeFile (c, data, len);
			}
			delete data;
		}
	}

	public void NPServerAddTask (int task, void *data)
	{
		TaskQueue [TaskTail].Task = task;
		TaskQueue [TaskTail].Data = data;

		TaskTail = (TaskTail + 1) % NP_MAX_TASKS;
	}

	public void NPReset ()
	{
		NPNoClientReady (0);
		NPServerAddTask (NP_SERVER_RESET_ALL, 0);
	}

	public void NPWaitForEmulationToComplete ()
	{

		while (!NetPlay.PendingWait4Sync && NetPlay.Connected &&
			   !Settings.ForcedPause && !Settings.StopEmulation &&
			   !(Settings.Paused && !Settings.FrameAdvance))
		{

		}

	}

	public void NPServerQueueSyncAll ()
	{
		if (Settings.NetPlay && Settings.NetPlayServer &&
			NumClients > NP_ONE_CLIENT)
		{
			NPNoClientReady ();
			NPDiscardHeartbeats ();
			NPServerAddTask (NP_SERVER_SYNC_ALL, 0);
		}
	}

	public void NPServerQueueSendingROMImage ()
	{
		if (Settings.NetPlay && Settings.NetPlayServer &&
			NumClients > NP_ONE_CLIENT)
		{
			NPNoClientReady ();
			NPDiscardHeartbeats ();
			NPServerAddTask (NP_SERVER_SEND_ROM_IMAGE, 0);
		}
	}

	public void NPServerQueueSendingFreezeFile (String filename)
	{
		if (Settings.NetPlay && Settings.NetPlayServer &&
			NumClients > NP_ONE_CLIENT)
		{
			NPNoClientReady ();
			NPDiscardHeartbeats ();
			NPServerAddTask (NP_SERVER_SEND_FREEZE_FILE_ALL,
								(void *) strdup (filename));
		}
	}

	public void NPServerQueueSendingLoadROMRequest (String filename)
	{
		if (Settings.NetPlay && Settings.NetPlayServer &&
			NumClients > NP_ONE_CLIENT)
		{
			NPNoClientReady ();
			NPDiscardHeartbeats ();
			NPServerAddTask (NP_SERVER_SEND_ROM_LOAD_REQUEST_ALL,
								(void *) strdup (filename));
		}
	}
	*/
}
