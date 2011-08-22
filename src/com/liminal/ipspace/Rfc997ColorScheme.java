/*
 * Created on 19-Mar-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

import com.liminal.ipspace.whois.RFC997Reader;

public class Rfc997ColorScheme extends BlackColorScheme {

	private int researchOutlineColor;
	private int researchFillColor;
	private int defenseOutlineColor;
	private int defenseFillColor;
	private int governmentOutlineColor;
	private int governmentFillColor;
	private int commercialOutlineColor;
	private int commercialFillColor;
	private int reservedOutlineColor;
	private int reservedFillColor;
	private int unassignedOutlineColor;
	private int unassignedFillColor;
	
	public Rfc997ColorScheme() {
		
		IPSpace ipspace = IPSpace.getInstance();
		
//		researchOutlineColor = afrinicOutlineColor;
//		researchFillColor = afrinicFillColor;
//		defenseOutlineColor = apnicOutlineColor;
//		defenseFillColor = apnicFillColor;
//		governmentOutlineColor = arinOutlineColor;
//		governmentFillColor = arinFillColor;
//		commercialOutlineColor = ripeOutlineColor;
//		commercialFillColor = ripeFillColor;
		float fillAlpha = 100f;
		float outlineAlpha = 200f;
		int outlineColor = ipspace.color(0f, 0f, 255f, outlineAlpha);
		int fillColor = ipspace.color(0f, 0f, 255f, fillAlpha);
		researchOutlineColor = outlineColor;
		researchFillColor = fillColor;
		defenseOutlineColor = outlineColor;
		defenseFillColor = fillColor;
		governmentOutlineColor = outlineColor;
		governmentFillColor = fillColor;
		commercialOutlineColor = outlineColor;
		commercialFillColor = fillColor;
		reservedOutlineColor = ipspace.color(170, 128); // ianaFillColor;
		reservedFillColor = ipspace.color(170, 60); // ianaFillColor;
		unassignedOutlineColor = ipspace.color(60, outlineAlpha);
		unassignedFillColor = ipspace.color(60, fillAlpha);
//		researchOutlineColor = ipspace.color(255, 0, 0, outlineAlpha);
//		researchFillColor = ipspace.color(255, 0, 0, fillAlpha);
//		defenseOutlineColor = ipspace.color(0, 255, 0, outlineAlpha);
//		defenseFillColor = ipspace.color(0, 255, 0, fillAlpha);
//		governmentOutlineColor = ipspace.color(0, 0, 255, outlineAlpha);
//		governmentFillColor = ipspace.color(0, 0, 255, fillAlpha);
//		commercialOutlineColor = ipspace.color(255, 255, 0, outlineAlpha);
//		commercialFillColor = ipspace.color(255, 255, 0, fillAlpha);
//		reservedOutlineColor = ipspace.color(170); // ianaOutlineColor
//		reservedFillColor = ipspace.color(170, 60); // ianaFillColor;
//		unassignedOutlineColor = ipspace.color(60);
//		unassignedFillColor = ipspace.color(60);
	}

	@Override
	public int getFillColor(VNet vnet) {
		return researchFillColor;
		// TODO: FIX THIS!!!
//		String type = vnet.getNet().getRegistryString();
//		if (type.equals(RFC997Reader.RESEARCH))
//			return researchFillColor;
//		else if (type.equals(RFC997Reader.DEFENSE))
//			return defenseFillColor;
//		else if (type.equals(RFC997Reader.GOVERNMENT))
//			return governmentFillColor;
//		else if (type.equals(RFC997Reader.COMMERCIAL))
//			return commercialFillColor;
//		else if (type.equals(RFC997Reader.RESERVED))
//			return reservedFillColor;
//		else if (type.equals(RFC997Reader.UNASSIGNED))
//			return unassignedFillColor;
//		return super.getFillColor(vnet);
	}

	@Override
	public int getOutlineColor(VNet vnet) {
		return researchOutlineColor;
		// TODO: FIX THIS!!!
//		String type = vnet.getNet().getRegistryString();
//		if (type.equals(RFC997Reader.RESEARCH))
//			return researchOutlineColor;
//		else if (type.equals(RFC997Reader.DEFENSE))
//			return defenseOutlineColor;
//		else if (type.equals(RFC997Reader.GOVERNMENT))
//			return governmentOutlineColor;
//		else if (type.equals(RFC997Reader.COMMERCIAL))
//			return commercialOutlineColor;
//		else if (type.equals(RFC997Reader.RESERVED))
//			return reservedOutlineColor;
//		else if (type.equals(RFC997Reader.UNASSIGNED))
//			return unassignedOutlineColor;
//		return super.getOutlineColor(vnet);
	}

	
	
}
