/*
 * Created on 26-Feb-2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.ipspace;

public interface ColorScheme {

	int getBackgroundColor();
	
	int getOutlineColor(VNet vnet);
	
	int getPointColor(VNet vnet);
	
	int getFillColor(VNet vnet);
	
	int getPickOutlineColor(VNet vnet);
	int getPickFillColor(VNet vnet);

	int getNumberLineColor();
	
	int getHostColor();
}
