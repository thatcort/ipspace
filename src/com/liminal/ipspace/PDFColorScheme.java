/*
 * File:      PDFColorScheme.java
 * Created:   30-Jun-2006
 * Author:    bcort
 *
 */
package com.liminal.ipspace;

import static com.liminal.ipspace.data.Registry.AFRINIC;
import static com.liminal.ipspace.data.Registry.APNIC;
import static com.liminal.ipspace.data.Registry.ARIN;
import static com.liminal.ipspace.data.Registry.IANA;
import static com.liminal.ipspace.data.Registry.JPNIC;
import static com.liminal.ipspace.data.Registry.LACNIC;
import static com.liminal.ipspace.data.Registry.RIPE;

import com.liminal.ipspace.data.Registry;

public class PDFColorScheme implements ColorScheme {

	private IPSpace ipspace;
	
	protected int backgroundColor;
	
	protected int internetOutlineColor;
	protected int afrinicOutlineColor;
	protected int apnicOutlineColor;
	protected int arinOutlineColor;
	protected int ianaOutlineColor;
	protected int ripeOutlineColor;
	protected int lacnicOutlineColor;
	protected int jpnicOutlineColor;
	protected int defaultOutlineColor;
	protected int afrinicFillColor;
	protected int apnicFillColor;
	protected int arinFillColor;
	protected int ianaFillColor;
	protected int ripeFillColor;
	protected int lacnicFillColor;
	protected int jpnicFillColor;
	protected int defaultFillColor;
	protected int afrinicPointColor;
	protected int apnicPointColor;
	protected int arinPointColor;
	protected int ianaPointColor;
	protected int ripePointColor;
	protected int lacnicPointColor;
	protected int jpnicPointColor;
	protected int defaultPointColor;
	protected int unallocatedPointColor;	

	protected int pickOutlineColor;
	protected int pickFillColor;
	
	protected int numberLineColor;
	
	protected int hostColor;
	
	public PDFColorScheme(IPSpace ipspace) {
		this.ipspace = ipspace;
		
		backgroundColor = ipspace.color(0f);
		
		float outlineAlpha = 255f;
		float fillAlpha = 255f;
		float pointAlpha = 255;
		
		internetOutlineColor = ipspace.color(255f);
		afrinicOutlineColor = ipspace.color(255f, 0f, 0f, outlineAlpha);
		apnicOutlineColor = ipspace.color(0f, 255f, 0f, outlineAlpha);
		arinOutlineColor = ipspace.color(0f, 0f, 255f, outlineAlpha);
		ianaOutlineColor = ipspace.color(100f, outlineAlpha);
		ripeOutlineColor = ipspace.color(255f, 255f, 0f, outlineAlpha);
		lacnicOutlineColor = ipspace.color(0f, 255f, 255f, outlineAlpha);
		jpnicOutlineColor = ipspace.color(153f, 102f, 0f, outlineAlpha);
		defaultOutlineColor  = ipspace.color(255, 249, 214, outlineAlpha);
		afrinicFillColor  = ipspace.color(60f, 0f, 0f, fillAlpha);
		apnicFillColor	  = ipspace.color(0f, 60f, 0f, fillAlpha);
		arinFillColor	  = ipspace.color(0f, 0f, 60f, fillAlpha);
		ianaFillColor	  = ipspace.color(60f, 60f, 60f, fillAlpha);
		ripeFillColor	  = ipspace.color(60f, 60f, 0f, fillAlpha);
		lacnicFillColor	  = ipspace.color(0f, 60f, 60f, fillAlpha);
		jpnicFillColor = ipspace.color(89f, 57f, 11f, fillAlpha);
		defaultFillColor  = ipspace.color(255, 249, 214, fillAlpha);
		afrinicPointColor  = ipspace.color(255f, 0f, 0f, pointAlpha);
		apnicPointColor	  = ipspace.color(0f, 255f, 0f, pointAlpha);
		arinPointColor	  = ipspace.color(0f, 0f, 255f, pointAlpha);
		ianaPointColor	  = ipspace.color(128f, 128f, 128f, 255f);
		ripePointColor	  = ipspace.color(255f, 255f, 0f, pointAlpha);
		lacnicPointColor	  = ipspace.color(0f, 255f, 255f, pointAlpha);
		jpnicPointColor = ipspace.color(153f, 102f, 0f, pointAlpha);
		defaultPointColor  = ipspace.color(255, 249, 214, pointAlpha);
		unallocatedPointColor  = ipspace.color(128f, 128f, 128f, 150f);
		
		pickOutlineColor = ipspace.color(255f);
		pickFillColor = ipspace.color(255f, 255f, 255f, 40f);
		
		numberLineColor = ipspace.color(128f, 128f, 128f, 200f);
		
		hostColor = ipspace.color(140f, 140f, 100f);
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public int getOutlineColor(VNet vnet) {
		Registry reg = vnet.getNet().getRegistry();
		if (reg.equals(AFRINIC))
			return afrinicOutlineColor;
		else if (reg.equals(APNIC))
			return apnicOutlineColor;
		else if (reg.equals(ARIN))
			return arinOutlineColor;
		else if (reg.equals(IANA))
			return ianaOutlineColor;
		else if (reg.equals(LACNIC))
			return lacnicOutlineColor;
		else if (reg.equals(RIPE))
			return ripeOutlineColor;
		else if (reg.equals(JPNIC))
			return apnicOutlineColor; // jpnicOutlineColor;
		else if (vnet.getParent() == null)
			return internetOutlineColor;
		else
			return defaultOutlineColor;
	}

	public int getFillColor(VNet vnet) {
		Registry reg = vnet.getNet().getRegistry();
		if (reg.equals(AFRINIC))
			return afrinicFillColor;
		else if (reg.equals(APNIC))
			return apnicFillColor;
		else if (reg.equals(ARIN))
			return arinFillColor;
		else if (reg.equals(IANA))
			return ianaFillColor;
		else if (reg.equals(LACNIC))
			return lacnicFillColor;
		else if (reg.equals(RIPE))
			return ripeFillColor;
		else if (reg.equals(JPNIC))
			return apnicFillColor; // jpnicFillColor;
		else
			return defaultFillColor;
	}

	public int getPickOutlineColor(VNet vnet) {
		return pickOutlineColor;
	}

	public int getPickFillColor(VNet vnet) {
		return pickFillColor;
	}
	
	public int getNumberLineColor() {
		return numberLineColor;
	}
	
	public int getHostColor() {
		return hostColor;
	}
	
	public int getPointColor(VNet vnet) {
		if (!vnet.getNet().isAllocated())
			return unallocatedPointColor;
		Registry reg = vnet.getNet().getRegistry();
		if (reg.equals(AFRINIC))
			return afrinicPointColor;
		else if (reg.equals(APNIC))
			return apnicPointColor;
		else if (reg.equals(ARIN))
			return arinPointColor;
		else if (reg.equals(IANA))
			return ianaPointColor;
		else if (reg.equals(LACNIC))
			return lacnicPointColor;
		else if (reg.equals(RIPE))
			return ripePointColor;
		else if (reg.equals(JPNIC))
			return apnicPointColor; // jpnicPointColor;
		else
			return defaultPointColor;
	}

}