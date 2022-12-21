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

import java.io.Serializable;

public class Meta implements Serializable {
	public String url;
	public String Ordnungsnr;
	public String Erlasstitel;
	public String Kurztitel;
	public String Erlassdatum;
	public String Inkraftsetzungam;
	public String Aufhebungam;
	public String Bandnr;
	public String Nachtragnr;
	public String Link;
	public String Publikationsdatum;
	public String Materialien;
	
	public Meta( String url ) {
		this.url = url;
	}
}
