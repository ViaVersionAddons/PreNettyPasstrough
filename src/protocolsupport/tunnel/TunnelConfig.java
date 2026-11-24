package protocolsupport.tunnel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class TunnelConfig {

	private static volatile String server = "";
	private static volatile boolean includeLegacy = false;
	private static volatile boolean log = true;

	public static void load(File dataFolder, InputStream defaults) throws IOException {
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		File file = new File(dataFolder, "config.yml");
		if (!file.exists() && (defaults != null)) {
			Files.copy(defaults, file.toPath());
		}
		Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
		server = config.getString("tunnel.server", "");
		includeLegacy = config.getBoolean("tunnel.include-1-4-7-to-1-6-4", false);
		log = config.getBoolean("tunnel.log", true);
	}

	public static String getServer() {
		return server;
	}

	public static boolean isLog() {
		return log;
	}

	public static boolean tunnelsOldHandshakeProtocol(int protocolId) {
		if (protocolId < 51) {
			return true;
		}
		return includeLegacy && (protocolId <= 78);
	}

}
