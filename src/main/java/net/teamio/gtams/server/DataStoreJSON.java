package net.teamio.gtams.server;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.teamio.gtams.server.info.GoodsList;
import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.TradeDescriptor;
import net.teamio.gtams.server.info.TradeList;
import net.teamio.gtams.server.info.Transaction;
import net.teamio.gtams.server.storeentities.Player;
import net.teamio.gtams.server.storeentities.Terminal;

public class DataStoreJSON extends DataStore {

	private Gson gson;

	@Override
	protected void loadCache() {
		this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().registerTypeHierarchyAdapter(byte[].class, new ByteArrayBase64Adapter()).create();

		FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if(!pathname.isFile()) {
					return false;
				}
				return pathname.getName().endsWith(".json");
			}
		};

		System.out.println("Loading Terminals");
		File dir = new File("data/terminal");
		if(dir.exists()) {
			File[] children = dir.listFiles(fileFilter);

			for(File file : children) {
				System.out.println("Reading data/terminal/" + file.getName());
				try {
					Terminal term = readFile(Terminal.class, file);
					if(term != null && term.id != null) {
						addTerminalInternal(term);
					}
				} catch (GTamsServerException e) {
					System.err.println("Error reading terminal from " + file.getName());
					e.printStackTrace();
				} catch(DuplicateKeyException e) {
					System.err.println("Error reading terminal from " + file.getName());
					e.printStackTrace();
				}
			}
		}

		System.out.println("Loading Trade Descriptors (inactive)");
		dir = new File("data/descriptor");
		if(dir.exists()) {
			File[] children = dir.listFiles(fileFilter);

			for(File file : children) {
				System.out.println("Reading data/descriptor/" + file.getName());
				try {
					TradeDescriptor descriptor = readFile(TradeDescriptor.class, file);
					if(descriptor != null) {
						inactiveDescriptors.add(descriptor);
					}
				} catch (GTamsServerException e) {
					System.err.println("Error reading terminal from " + file.getName());
					e.printStackTrace();
				} catch(DuplicateKeyException e) {
					System.err.println("Error reading terminal from " + file.getName());
					e.printStackTrace();
				}
			}
		}

		System.out.println("Loading Trades");
		dir = new File("data/terminal_trades");
		if(dir.exists()) {
			File[] children = dir.listFiles(fileFilter);

			for(File file : children) {
				System.out.println("Reading data/terminal_trades/" + file.getName());
				TradeList list;
				try {
					list = readFile(TradeList.class, file);

					if(list != null && list.trades != null) {
						for(Trade trade : list.trades) {
							if(trade != null && trade.terminalId != null) {
								addTradeInternal(trade);
							}
						}
					}
				} catch (GTamsServerException e) {
					System.err.println("Error reading terminal trades from " + file.getName());
					e.printStackTrace();
				} catch(DuplicateKeyException e) {
					System.err.println("Error reading terminal trades from " + file.getName());
					e.printStackTrace();
				}
			}
		}

		System.out.println("Loading Goods");
		dir = new File("data/terminal_goods");
		if(dir.exists()) {
			File[] children = dir.listFiles(fileFilter);

			for(File file : children) {
				System.out.println("Reading data/terminal_goods/" + file.getName());
				GoodsList list;
				try {
					list = readFile(GoodsList.class, file);

					if(list != null && list.goods != null && list.terminalId != null) {
						addGoodsInternal(list.terminalId, list.goods);
					}
				} catch (GTamsServerException e) {
					System.err.println("Error reading terminal goods from " + file.getName());
					e.printStackTrace();
				} catch(DuplicateKeyException e) {
					System.err.println("Error reading terminal goods from " + file.getName());
					e.printStackTrace();
				}
			}
		}
		System.out.println("Done loading.");
	}

	private File getFile(String category, String name) {
		String fileName = String.format("data/%s/%s.json", category, name);
		return new File(fileName);
	}

	private void deleteFile(String category, String name) throws GTamsServerException {
		File fl = getFile(category, name);
		if(fl.exists()) {
			if(!fl.delete()) {
				throw new GTamsServerException("Error deleting file: " + fl.getAbsolutePath());
			}
		}
	}

	private FileOutputStream getFileStream(String category, String name) throws GTamsServerException {
		File file = getFile(category, name);
		try {
			file.getParentFile().mkdirs();
			return new FileOutputStream(file, false);
		} catch (FileNotFoundException e) {
			throw new GTamsServerException("Failed to open file for data output: " + file.getAbsolutePath(), e);
		}
	}

	private <T> T readFile(Class<T> dataClass, File file) throws GTamsServerException {
		try(FileInputStream fis = new FileInputStream(file)) {
			try(InputStreamReader isr = new InputStreamReader(fis, "utf8")) {
				return gson.fromJson(isr, dataClass);
			}
		} catch (IOException e) {
			throw new GTamsServerException("Failed to read from file: " + file.getAbsolutePath(), e);
		} catch(JsonSyntaxException e) {
			throw new GTamsServerException("Failed to read from file (JSON Syntax): " + file.getAbsolutePath(), e);
		}
	}

	private void writeFile(Object data, String category, String name) throws GTamsServerException {
		try(FileOutputStream fos = getFileStream(category, name)) {
			try(OutputStreamWriter osw = new OutputStreamWriter(fos, "utf8")) {
				gson.toJson(data, osw);
			}
		} catch (IOException e) {
			throw new GTamsServerException("Could not write data to output stream.", e);
		}
	}

	@Override
	protected void saveTerminal(Terminal terminal) {
		try {
			writeFile(terminal, "terminal", terminal.id.toString());
		} catch (GTamsServerException e) {
			System.err.println("Error writing terminal");
			e.printStackTrace();
		}
	}

	@Override
	protected void deleteSavedTerminal(UUID id) {
		String name = id.toString();
		try {
			deleteFile("terminal", name);
		} catch (GTamsServerException e) {
			System.err.println("Error deleting terminal");
			e.printStackTrace();
		}
		try {
			deleteFile("terminal_trades", name);
		} catch (GTamsServerException e) {
			System.err.println("Error deleting terminal trades");
			e.printStackTrace();
		}
		try {
			deleteFile("terminal_goods", name);
		} catch (GTamsServerException e) {
			System.err.println("Error deleting terminal goods");
			e.printStackTrace();
		}

	}

	@Override
	protected void saveTrades(UUID terminalId) {
		Collection<Trade> trades = getTradesForTerminal(terminalId);
		TradeList list = new TradeList(trades);
		try {
			writeFile(list, "terminal_trades", terminalId.toString());
		} catch (GTamsServerException e) {
			System.err.println("Error writing terminal trades");
			e.printStackTrace();
		}
	}

	@Override
	protected void saveGoods(UUID terminalId) {
		GoodsList list = getGoods(terminalId);
		try {
			list.terminalId = terminalId;
			writeFile(list, "terminal_goods", terminalId.toString());
		} catch (GTamsServerException e) {
			System.err.println("Error writing terminal goods");
			e.printStackTrace();
		}
	}

	@Override
	protected void saveDescriptor(TradeDescriptor descriptor) {
		try {
			writeFile(descriptor, "descriptor", descriptor.toFilename());
		} catch (GTamsServerException e) {
			System.err.println("Error writing descriptor");
			e.printStackTrace();
		}
	}

	@Override
	protected void savePlayer(Player player) {
		try {
			writeFile(player, "player", player.id.toString());
		} catch (GTamsServerException e) {
			System.err.println("Error writing player");
			e.printStackTrace();
		}
	}

	@Override
	protected void addTransaction(Transaction transaction) {
		// TODO Auto-generated method stub

	}

}
