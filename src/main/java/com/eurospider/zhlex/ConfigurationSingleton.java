/*
 * Copyright (C) 2013 LEXspider <lexspider@eurospider.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.eurospider.zhlex;

import org.apache.commons.configuration.PropertiesConfiguration;

public class ConfigurationSingleton {
	
	private static ConfigurationSingleton instance;
	private ConfigurationSingleton(){}
	
	
	private static PropertiesConfiguration configuration;
	
	public static ConfigurationSingleton getConfigInstance()
	{
		if(ConfigurationSingleton.instance == null)
		{
			ConfigurationSingleton.instance = new ConfigurationSingleton();
		}
		
		return ConfigurationSingleton.instance;
	}
	
	public PropertiesConfiguration getConfiguration()
	{
		return ConfigurationSingleton.configuration;
	}

	public void setConfiguration(PropertiesConfiguration configuration) 
	{
		if(ConfigurationSingleton.configuration == null)
		{
			ConfigurationSingleton.configuration = configuration;
		}
	}

	
}
