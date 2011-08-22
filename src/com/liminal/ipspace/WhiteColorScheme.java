package com.liminal.ipspace;

import com.liminal.ipspace.whois.WhoisManager;

public class WhiteColorScheme { 
//implements ColorScheme {
//
//	protected int backgroundColor;
//	
//	protected int internetOutlineColor;
//	protected int afrinicOutlineColor;
//	protected int apnicOutlineColor;
//	protected int arinOutlineColor;
//	protected int ianaOutlineColor;
//	protected int ripeOutlineColor;
//	protected int lacnicOutlineColor;
//	protected int defaultOutlineColor;
//	protected int afrinicFillColor;
//	protected int apnicFillColor;
//	protected int arinFillColor;
//	protected int ianaFillColor;
//	protected int ripeFillColor;
//	protected int lacnicFillColor;
//	protected int defaultFillColor;
//	protected int afrinicPointColor;
//	protected int apnicPointColor;
//	protected int arinPointColor;
//	protected int ianaPointColor;
//	protected int ripePointColor;
//	protected int lacnicPointColor;
//	protected int defaultPointColor;
//	protected int unallocatedPointColor;
//	
//	protected int pickOutlineColor;
//	protected int pickFillColor;
//	
//	protected int numberLineColor;
//	
//	protected int hostColor;
//	
//	public WhiteColorScheme() {
//
//		IPSpace ipspace = IPSpace.getInstance();
//		backgroundColor = ipspace.color(255f);
//		
//		float outlineAlpha = 125f;
//		float fillAlpha = 200f;
//		float pointAlpha = 220;
//		
//		float full = 100f;
//		float half = full * .5f;
//		
//		internetOutlineColor = ipspace.color(0f);
//		afrinicOutlineColor = ipspace.color(full, 0f, 0f, outlineAlpha);
//		apnicOutlineColor = ipspace.color(0f, full, 0f, outlineAlpha);
//		arinOutlineColor = ipspace.color(0f, 0f, full, outlineAlpha);
//		ianaOutlineColor = ipspace.color(100f, outlineAlpha);
//		ripeOutlineColor = ipspace.color(full, full, 0f, outlineAlpha);
//		lacnicOutlineColor = ipspace.color(0f, full, full, outlineAlpha);
//		defaultOutlineColor  = ipspace.color(255, 249, 214, outlineAlpha);
//		afrinicFillColor  = ipspace.color(full, 0f, 0f, fillAlpha);
//		apnicFillColor	  = ipspace.color(0f, full, 0f, fillAlpha);
//		arinFillColor	  = ipspace.color(0f, 0f, full, fillAlpha);
//		ianaFillColor	  = ipspace.color(half, half, half, fillAlpha);
//		ripeFillColor	  = ipspace.color(full, full, 0f, fillAlpha);
//		lacnicFillColor	  = ipspace.color(0f, full, full, fillAlpha);
//		defaultFillColor  = ipspace.color(255, 249, 214, fillAlpha);
//		afrinicPointColor  = ipspace.color(full, 0f, 0f, pointAlpha);
//		apnicPointColor	  = ipspace.color(0f, full, 0f, pointAlpha);
//		arinPointColor	  = ipspace.color(0f, 0f, full, pointAlpha);
//		ianaPointColor	  = ipspace.color(half, half, half, full);
//		ripePointColor	  = ipspace.color(full, full, 0f, pointAlpha);
//		lacnicPointColor	  = ipspace.color(0f, full, full, pointAlpha);
//		defaultPointColor  = ipspace.color(255, 249, 214, pointAlpha);
//		unallocatedPointColor  = ipspace.color(half, half, half, 150f);
//		
//		pickOutlineColor = ipspace.color(255f);
//		pickFillColor = ipspace.color(255f, 255f, 255f, 40f);
//		
//		numberLineColor = ipspace.color(128f, 128f, 128f, 200f);
//		
//		hostColor = ipspace.color(140f, 140f, 100f);
//	}
//
//	public int getBackgroundColor() {
//		return backgroundColor;
//	}
//
//	public int getOutlineColor(VNet vnet) {
//		String reg = vnet.getNet().getRegistryString();
//		if (reg.equals(WhoisManager.AFRINIC))
//			return afrinicOutlineColor;
//		else if (reg.equals(WhoisManager.APNIC))
//			return apnicOutlineColor;
//		else if (reg.equals(WhoisManager.ARIN))
//			return arinOutlineColor;
//		else if (reg.equals(WhoisManager.IANA))
//			return ianaOutlineColor;
//		else if (reg.equals(WhoisManager.LACNIC))
//			return lacnicOutlineColor;
//		else if (reg.equals(WhoisManager.RIPE))
//			return ripeOutlineColor;
//		else if (vnet.getParent() == null)
//			return internetOutlineColor;
//		else
//			return defaultOutlineColor;
//	}
//
//	public int getFillColor(VNet vnet) {
//		String reg = vnet.getNet().getRegistryString();
//		if (reg.equals(WhoisManager.AFRINIC))
//			return afrinicFillColor;
//		else if (reg.equals(WhoisManager.APNIC))
//			return apnicFillColor;
//		else if (reg.equals(WhoisManager.ARIN))
//			return arinFillColor;
//		else if (reg.equals(WhoisManager.IANA))
//			return ianaFillColor;
//		else if (reg.equals(WhoisManager.LACNIC))
//			return lacnicFillColor;
//		else if (reg.equals(WhoisManager.RIPE))
//			return ripeFillColor;
//		else
//			return defaultFillColor;
//	}
//
//	public int getPickOutlineColor(VNet vnet) {
//		return pickOutlineColor;
//	}
//
//	public int getPickFillColor(VNet vnet) {
//		return pickFillColor;
//	}
//	
//	public int getNumberLineColor() {
//		return numberLineColor;
//	}
//	
//	public int getHostColor() {
//		return hostColor;
//	}
//	
//	public int getPointColor(VNet vnet) {
//		if (!vnet.getNet().isAllocated())
//			return unallocatedPointColor;
//		String reg = vnet.getNet().getRegistryString();
//		if (reg.equals(WhoisManager.AFRINIC))
//			return afrinicPointColor;
//		else if (reg.equals(WhoisManager.APNIC))
//			return apnicPointColor;
//		else if (reg.equals(WhoisManager.ARIN))
//			return arinPointColor;
//		else if (reg.equals(WhoisManager.IANA))
//			return ianaPointColor;
//		else if (reg.equals(WhoisManager.LACNIC))
//			return lacnicPointColor;
//		else if (reg.equals(WhoisManager.RIPE))
//			return ripePointColor;
//		else
//			return defaultPointColor;
//	}
//

}
