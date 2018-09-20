/*
 * Copyright (c) 2014 Villu Ruusmann
 *
 * This file is part of Openscoring
 *
 * Openscoring is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *
 * (at your option) any later version.
 * Openscoring is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Openscoring.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscoring.server;

import java.io.File;
import java.net.InetSocketAddress;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;

import org.openscoring.service.Openscoring;
import org.openscoring.client.PollingDirectoryDeployer;

public class Main {

	@Parameter (
		names = {"--console-war"},
		description = "Console web application (WAR) file or directory",
		hidden = true
	)
	private File consoleWar = null;

	@Parameter (
		names = {"--context-path"},
		description = "Context path"
	)
	private String contextPath = "/openscoring";

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;

	@Parameter (
		names = {"--host"},
		description = "Server host name or ip address"
	)
	private String host = null;

	@Parameter (
		names = {"--port"},
		description = "Server port"
	)
	private int port = 8080;

	@Parameter (
	  names = {"--auto-deploy"},
		description = "Start the auto deployer thread"
	)
	private boolean startDeployment = false;

	@Parameter (
	  names = {"--deployer-polling"},
		description = "Enables the deployer polling method instead of inotify"
	)
	private boolean usePollingMethod = false;

	@Parameter (
		names = {"--poll-interval"},
		description = "Time in ms to wait between scan operatoins"
	)
	private long pollIntervalMs;

	@Parameter (
	  names = {"--model-dir"},
		description = "Directory to watch for auto deployment"
	)
	private File modelDir = null;


	static
	public void main(String... args) throws Exception {
		execute(Main.class, args);
	}

	static
	public void execute(Class<? extends Main> clazz, String... args) throws Exception {
		Main main = clazz.newInstance();

		JCommander commander = new JCommander(main);
		commander.setProgramName(clazz.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			commander.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			commander.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	public void run() throws Exception {
		InetSocketAddress address;

		if(this.host != null){
			address = new InetSocketAddress(this.host, this.port);
		} else

		{
			address = new InetSocketAddress(this.port);
		}

		Server server = createServer(address);

		if(startDeployment){
			if(usePollingMethod){
				PollingDirectoryDeployer pollingDeployer = new PollingDirectoryDeployer();

				String modelHost = this.host == null ? "localhost" : this.host;

				String modelCollectionUrl = String.format("http://%s:%d%s/model", modelHost, this.port, this.contextPath);

				System.out.println("Model colleciton URL is : " + modelCollectionUrl);
				pollingDeployer.setModelCollection(modelCollectionUrl);
				pollingDeployer.setDir(this.modelDir);
				pollingDeployer.setPollIntervalMs(this.pollIntervalMs);

				Thread deployPollThread = new Thread(pollingDeployer);
				deployPollThread.start();
			}
		}

		server.start();
		server.join();
	}

	private Server createServer(InetSocketAddress address){
		Server server = new Server(address);

		Openscoring application = new Openscoring();

		ServletContainer jerseyServlet = new ServletContainer(application);

		ServletContextHandler servletHandler = new ServletContextHandler();
		servletHandler.setContextPath(this.contextPath);

		servletHandler.addServlet(new ServletHolder(jerseyServlet), "/*");

		ContextHandlerCollection handlerCollection = new ContextHandlerCollection();

		handlerCollection.addHandler(servletHandler);

		if(this.consoleWar != null){
			WebAppContext consoleHandler = new WebAppContext();
			consoleHandler.setContextPath(this.contextPath + "/console"); // XXX
			consoleHandler.setWar(this.consoleWar.getAbsolutePath());

			handlerCollection.addHandler(consoleHandler);
		}

		server.setHandler(handlerCollection);

		return server;
	}
}
