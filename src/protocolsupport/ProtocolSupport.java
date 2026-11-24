package protocolsupport;

import java.util.logging.Level;

import net.md_5.bungee.api.plugin.Plugin;
import protocolsupport.injector.BungeeNettyChannelInjector;

public class ProtocolSupport extends Plugin {

	@Override
	public void onLoad() {
		try {
			protocolsupport.tunnel.TunnelConfig.load(getDataFolder(), getResourceAsStream("config.yml"));
			getProxy().getPluginManager().registerCommand(this, new CommandHandler());
			BungeeNettyChannelInjector.inject();
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Error occured while initalizing", t);
		}
	}


}
