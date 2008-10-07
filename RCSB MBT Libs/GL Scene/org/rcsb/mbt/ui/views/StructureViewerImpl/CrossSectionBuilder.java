//  $Id: CrossSectionBuilder.java,v 1.1 2007/02/08 02:38:52 jbeaver Exp $
//
//  Copyright 2000-2004 The Regents of the University of California.
//  All Rights Reserved.
//
//  Permission to use, copy, modify and distribute any part of this
//  Molecular Biology Toolkit (MBT)
//  for educational, research and non-profit purposes, without fee, and without
//  a written agreement is hereby granted, provided that the above copyright
//  notice, this paragraph and the following three paragraphs appear in all
//  copies.
//
//  Those desiring to incorporate this MBT into commercial products
//  or use for commercial purposes should contact the Technology Transfer &
//  Intellectual Property Services, University of California, San Diego, 9500
//  Gilman Drive, Mail Code 0910, La Jolla, CA 92093-0910, Ph: (858) 534-5815,
//  FAX: (858) 534-7345, E-MAIL:invent@ucsd.edu.
//
//  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
//  DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING
//  LOST PROFITS, ARISING OUT OF THE USE OF THIS MBT, EVEN IF THE
//  UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//  THE MBT PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE
//  UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
//  UPDATES, ENHANCEMENTS, OR MODIFICATIONS. THE UNIVERSITY OF CALIFORNIA MAKES
//  NO REPRESENTATIONS AND EXTENDS NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR
//  EXPRESS, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE, OR THAT THE USE OF THE
//  MBT WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.
//
//  For further information, please see:  http://mbt.sdsc.edu
//
//  History:
//  $Log: CrossSectionBuilder.java,v $
//  Revision 1.1  2007/02/08 02:38:52  jbeaver
//  version 1.50
//
//  Revision 1.2  2006/10/04 17:21:06  jbeaver
//  Lots of changes from surfaces to improved picking
//
//  Revision 1.1  2006/09/20 16:50:43  jbeaver
//  first commit - branched from ProteinWorkshop
//
//  Revision 1.1  2006/08/24 17:39:03  jbeaver
//  *** empty log message ***
//
//  Revision 1.1  2006/03/09 00:18:55  jbeaver
//  Initial commit
//
//  Revision 1.6  2004/05/24 21:39:39  agramada
//  Corrected a spelling error in a style name.
//
//  Revision 1.5  2004/04/09 00:04:28  moreland
//  Updated copyright to new UCSD wording.
//
//  Revision 1.4  2004/01/29 18:05:20  agramada
//  Removed General Atomics from copyright
//
//  Revision 1.3  2003/07/18 21:32:54  agramada
//  Fixed the code to handle the cases when no colors are set in the style.
//
//  Revision 1.2  2003/06/25 21:34:22  agramada
//  Removed other components not needed with the new api.
//
//  Revision 1.1  2003/06/24 22:19:45  agramada
//  Reorganized the geometry package. Old classes removed, new classes added.
//
//


//***********************************************************************************
// Package
//***********************************************************************************
//
package org.rcsb.mbt.ui.views.StructureViewerImpl;

import org.rcsb.mbt.glscene.geometry.Quat4f;
import org.rcsb.mbt.glscene.geometry.Vector3f;
import org.rcsb.mbt.glscene.jogl.Color3f;
import org.rcsb.mbt.ui.views.StructureViewerImpl.vec.*;


/**
 * CrossSectionBuilder class: produces closed polygons based on the description in a given CrossSectionStyle.  
 * <P>
 * @author      Apostol Gramada
 */
public class CrossSectionBuilder {

    /**
     * Return a IntrinsicCrossSection polygon
     */
    public static IntrinsicCrossSection getIntrinsicCrossSection( final CrossSectionStyle style, final Vector3f normal ) {

	// If a known style, call the appropriate method
	//
	if( style.getStyleName() == "REGULAR_POLYGON" ) {
	    return CrossSectionBuilder.getIntrinsicRegularPolygon( style ); 
	}
	if( style.getStyleName() == "ROUNDED_TUBE" ) {
	    return CrossSectionBuilder.getIntrinsicRegularPolygon( style ); 
	}
	if( style.getStyleName() == "RECTANGULAR_RIBBON" ) {
	    return CrossSectionBuilder.getIntrinsicRectangle( style, 5 ); 
	}
	else if( style.getStyleName() == "DOUBLE_FACE" ) {
	    return CrossSectionBuilder.getIntrinsicDoubleFace( style, 3, normal );
	}
	else if( style.getStyleName() == "SINGLE_FACE" ) {
	    return CrossSectionBuilder.getIntrinsicSingleFace( style, 2, normal );
	}
	else if( style.getStyleName() == "OBLONG" ) {
	    return CrossSectionBuilder.getIntrinsicOblong( style, normal );
	}
	else {
	}

	return new IntrinsicCrossSection();
    }

     //
    // Methods that generate Cross Section of different styles
    //
    // Generate an INTRINSIC regular polygon cross section (i.e. in the LOCAL reference frame).
    private static IntrinsicCrossSection getIntrinsicRegularPolygon( final CrossSectionStyle style ) {
	//
        // CONVENTION: The first axes is the tangent (versor: t), 
        // CONVENTION: the second one is the normal (versor: n)
        // CONVENTION: and the third one is the binormal (versor: b).
        //
	// The dificult part is to set a first point of the base poligon.
        // In the future this issue may be settled based on the residue orientation.
	// For now, Start with an arbitrary point, say delta in the (n,b) plane.
	// Then rotate and/or rescale such that the final position is consistent
	// with the desired SSState.
        //
	final int vert = style.getVertexCount();
	final int facets = vert-1;
	final float radius = style.getDiameters()[0]/2;
	Vector3f nNormalized = null;
	final Vector3f[] delta = new Vector3f[vert];
	final Vec3f[] v = new Vec3f[vert];
	Vector3f n = null;
	final Vector3f[] vv = new Vector3f[vert];
	n = new Vector3f( 1.0f, 0.0f, 0.0f );
	nNormalized = new Vector3f( n ); // Save it.

	delta[0] = new Vector3f( 0.0f, 2.0f, 2.0f ); // Perpendicular to n.
	
	// Shape specifics come into play now.
	// Assume an EVEN number of sides. 
	// Also assume height and width are equal. Otherwise,
	// it couldn't be a regular polygon. For simplicity, their common values is interpreted here  
	// as the CIRCUMRADIUS (the radius of an circumscribed circle).
	// Then we just have to scale by the height value.
	delta[0].normalize();
	delta[0].scale( radius ); // This should be a good first corner of the polygon.

	Color3f currentColor = null;
	if( style.getVertexColors() != null ) {
	    currentColor = style.getVertexColors()[0];
	}
	else {
	    //currentColor = new Color3f( 1.0f, 1.0f, 1.0f );
	}

	// Set the first and last vertices. 
	// ( Closed poligon => first and last vertices coincide. )
	final Vector3f localOrigin = new Vector3f( 0.0f, 0.0f, 0.0f );
	vv[0] = new Vector3f( localOrigin );
	vv[0].add( delta[0] );
	vv[vert-1] = vv[0];

	v[0] = new Vec3f();
	v[0].value[0] = vv[0].coordinates[0];
	v[0].value[1] = vv[0].coordinates[1];
	v[0].value[2] = vv[0].coordinates[2];
	v[vert-1] = v[0];
	
	// Now, in principle, we would have to generate n-1 other vectors in the plane perpendicular to 
	// the normal by rotating the one just generated by an appropriate angle.
	// The angle of rotation is related to the number of sides by the following formula
	//
	//// theta = PI/n ( half the cetral angle associated with one side ).
	//
	// Construct the quaternion:
	float cosHalfT, sinHalfT; // The set of angles
	float x, y, z;
	cosHalfT = (float)Math.cos( -Math.PI/facets );
	sinHalfT = (float)Math.sin( -Math.PI/facets );

	// Prepare the quaternions needed for rotations.
	x = sinHalfT*nNormalized.coordinates[0];
	y = sinHalfT*nNormalized.coordinates[1];
	z = sinHalfT*nNormalized.coordinates[2];
	final Quat4f q1 =  new Quat4f( x, y, z, cosHalfT );

	// Need some working space
	final Quat4f q = new Quat4f();
	final Quat4f p = new Quat4f();

	for( int i=1; i < vert-1; i++ ) {
	    q.set( delta[i-1].coordinates[0], delta[i-1].coordinates[1], delta[i-1].coordinates[2], 0.0f );
	    p.set( q1 );
	    q.mulInverse( q1 );
	    p.mul( q );

	    // Extract the new vector
	    delta[i] = new Vector3f( p.coordinates[0], p.coordinates[1], p.coordinates[2] );
	    vv[i] = new Vector3f( localOrigin );
	    v[i] = new Vec3f();
	    vv[i].add( delta[i] );
	    v[i].value[0] = vv[i].coordinates[0];
	    v[i].value[1] = vv[i].coordinates[1];
	    v[i].value[2] = vv[i].coordinates[2];
	}

	final IntrinsicCrossSection poligon = new IntrinsicCrossSection( vert, v );
	poligon.setNormalsByVector( nNormalized );

	if( currentColor != null ) {
	    final Color3f[] colors = new Color3f[vert];
	    //	Color3f tmp = new Color3f( 1.0f, 0.5f, 0.0f );
	    final Color3f tmp = new Color3f( currentColor );
	    for( int i = 0; i < vert; i++ ) {
		colors[i] = tmp;
	    }
	    poligon.setColors( colors );
	    poligon.setSideColor( new Color3f( 0.3f, 0.3f, 0.3f ) );
	}

	return poligon;
    }

    // Generate an INTRINSIC rectangle.
    private static IntrinsicCrossSection  getIntrinsicRectangle( final CrossSectionStyle style, final int vert ) {
	//
        // CONVENTION: The first axes is the tangent (versor: t), 
        // CONVENTION: the second one is the normal (versor: n)
        // CONVENTION: and the third one is the binormal (versor: b).
        //
        // Start with a reference rectangle with the longer side in the direction of the binormal
        // and the shorter side in the direction of the normal. 
        //
	Vector3f nNormalized = null;
	final Vector3f[] delta = new Vector3f[vert];
	final Vec3f[] v = new Vec3f[vert];
	Vector3f n = null;
	final Vector3f[] vv = new Vector3f[vert];
	n = new Vector3f( 1.0f, 0.0f, 0.0f );
	nNormalized = new Vector3f( n ); // Save it.
	
	// Shape specifics come into play now.
	// Need a rectangle of semi-diagonal = sqrt(width**2 + height**2).
	final Vector3f localOrigin = new Vector3f( 0.0f, 0.0f, 0.0f );

	float width, height;
	width = style.getDiameters()[0];
	height = style.getDiameters()[1];
	Color3f currentColor = null;
	if( style.getVertexColors() != null) {
	    currentColor = style.getVertexColors()[0];
	}
	delta[0] = new Vector3f( 0.0f, -height/2.0f, width/2.0f ); // Perpendicular to t.
	final float s = (float)Math.sqrt( Math.pow( width, 2.0d) + Math.pow( height, 2.0d) );

	vv[0] = new Vector3f( localOrigin );
	vv[0].add( delta[0] );
	vv[vert-1] = vv[0];

	v[0] = new Vec3f();
	v[0].value[0] = vv[0].coordinates[0];
	v[0].value[1] = vv[0].coordinates[1];
	v[0].value[2] = vv[0].coordinates[2];
	v[vert-1] = v[0];
	
	// Now, in principle, we would have to generate 3 other vectors in the plane perpendicular to n
	// by rotating this one by an appropriate angle.
	// We need 2 quaternions representing the rotations corresponding to respectively the height and
	// width of the rectangle.
	float cosHalfT1, cosHalfT2, sinHalfT1, sinHalfT2; // The set of angles
	float x, y, z;
	
	sinHalfT2 = -width/s;
	cosHalfT1 = -sinHalfT2;
	sinHalfT1 = -height/s;
	cosHalfT2 = -sinHalfT1;
	
	// Prepare the quaternions needed for rotations.
	x = sinHalfT1*nNormalized.coordinates[0];
	y = sinHalfT1*nNormalized.coordinates[1];
	z = sinHalfT1*nNormalized.coordinates[2];
	final Quat4f q1 =  new Quat4f( x, y, z, cosHalfT1 );
	x = sinHalfT2*nNormalized.coordinates[0];
	y = sinHalfT2*nNormalized.coordinates[1];
	z = sinHalfT2*nNormalized.coordinates[2];
	final Quat4f q2 = new Quat4f( x, y, z, cosHalfT2);

	// Need some working space
	final Quat4f q = new Quat4f();
	final Quat4f p = new Quat4f();

	for( int i=1; i < vert-1; i++ ) {
	    q.set( delta[i-1].coordinates[0], delta[i-1].coordinates[1], delta[i-1].coordinates[2], 0.0f );
	    if( i%2 == 1) {
	      p.set( q1 );
	      q.mulInverse( q1 );
	      p.mul( q );
	    }
	    else {
	      p.set( q2 );
	      q.mulInverse( q2 );
	      p.mul( q );
	    }

	    // Extract the new vector
	    delta[i] = new Vector3f( p.coordinates[0], p.coordinates[1], p.coordinates[2] );
	    vv[i] = new Vector3f( localOrigin );
	    v[i] = new Vec3f();
	    vv[i].add( delta[i] );
	    v[i].value[0] = vv[i].coordinates[0];
	    v[i].value[1] = vv[i].coordinates[1];
	    v[i].value[2] = vv[i].coordinates[2];
	}

	final IntrinsicCrossSection poligon = new IntrinsicCrossSection( vert, v );
		
	poligon.setNormalsByVector( nNormalized );

	if( currentColor != null ) {
	    final Color3f[] colors = new Color3f[vert];
	    colors[0] = colors[2] = colors[4] = new Color3f( 0.3f, 0.3f, 0.3f );
	    colors[1] = colors[3] = new Color3f( currentColor );
	
	    poligon.setColors( colors );
	    poligon.setSideColor( colors[0] );
	}

	return poligon;
    }

    // Generate an INTRINSIC float face cross section.
    private static IntrinsicCrossSection  getIntrinsicOblong( final CrossSectionStyle style, final Vector3f normal ) {
	//
        // CONVENTION: The first axes is the tangent (versor: t), 
        // CONVENTION: the second one is the normal (versor: n)
        // CONVENTION: and the third one is the binormal (versor: b).
        //
        // Start with a reference rectangle with the longer side in the direction of the binormal
        // and the shorter side in the direction of the normal. 
        //
	// For the moment start with a shape made of 14 faces no matter what vert parameter is. 
	// 
	//int vert = style.getVertexCount(); // Override the parameter value;
	final int vert = 15;
	final float time1 = System.currentTimeMillis();
	Vector3f nNormalized = null;
	Vector3f delta = null;
	final Vec3f[] v = new Vec3f[vert];
	Vector3f n = null;
	final Vector3f[] vv = new Vector3f[vert];
	n = new Vector3f( 1.0f, 0.0f, 0.0f );
	nNormalized = new Vector3f( n ); // Save it.
	
	// Shape specifics come into play now.
	// Need a rectangle of semi-diagonal = sqrt(state.width**2 + state.height**2).
	final Vector3f localOrigin = new Vector3f( 0.0f, 0.0f, 0.0f );
	float width, height;
	width = style.getDiameters()[0];
	height = style.getDiameters()[1];
	Color3f currentColor = null;
	if( style.getVertexColors()[0] != null ) {
	    currentColor = style.getVertexColors()[0];
	}
	delta = new Vector3f( 0.0f, 0.0f, width/2.0f ); // Perpendicular to t.
	final float s = (float)Math.sqrt( Math.pow( width, 2.0d) + Math.pow(height, 2.0d) );

	vv[0] = new Vector3f( localOrigin );
	vv[0].add( delta );
	vv[vert-1] = vv[0];

	v[0] = new Vec3f();
	v[0].value[0] = vv[0].coordinates[0];
	v[0].value[1] = vv[0].coordinates[1];
	v[0].value[2] = vv[0].coordinates[2];
	v[vert-1] = v[0];
	
	delta = new Vector3f( 0.0f, +0.2f*height/2.0f, 0.9f*width/2.0f ) ;
	vv[1] = new Vector3f( localOrigin );
	vv[1].add( delta );
	v[1] = new Vec3f();
	v[1].value[0] = vv[1].coordinates[0];
	v[1].value[1] = vv[1].coordinates[1];
	v[1].value[2] = vv[1].coordinates[2];
	
	vv[13] = new Vector3f( vv[1] );
	vv[13].coordinates[1] = -vv[13].coordinates[1];
	v[13] = new Vec3f();
	v[13].value[0] = vv[13].coordinates[0];
	v[13].value[1] = vv[13].coordinates[1];
	v[13].value[2] = vv[13].coordinates[2];
		
	delta = new Vector3f( 0.0f, +0.8f*height/2.0f, 0.6f*width/2.0f ) ;
	vv[2] = new Vector3f( localOrigin );
	vv[2].add( delta );
	v[2] = new Vec3f();
	v[2].value[0] = vv[2].coordinates[0];
	v[2].value[1] = vv[2].coordinates[1];
	v[2].value[2] = vv[2].coordinates[2];

	vv[12] = new Vector3f( vv[2] );
	vv[12].coordinates[1] = -vv[12].coordinates[1];
	v[12] = new Vec3f();
	v[12].value[0] = vv[12].coordinates[0];
	v[12].value[1] = vv[12].coordinates[1];
	v[12].value[2] = vv[12].coordinates[2];
		
	delta = new Vector3f( 0.0f, +height/2.0f, 0.2f*width/2.0f ) ;
	vv[3] = new Vector3f( localOrigin );
	vv[3].add( delta );
	v[3] = new Vec3f();
	v[3].value[0] = vv[3].coordinates[0];
	v[3].value[1] = vv[3].coordinates[1];
	v[3].value[2] = vv[3].coordinates[2];

	vv[11] = new Vector3f( vv[3] );
	vv[11].coordinates[1] = -vv[11].coordinates[1];
	v[11] = new Vec3f();
	v[11].value[0] = vv[11].coordinates[0];
	v[11].value[1] = vv[11].coordinates[1];
	v[11].value[2] = vv[11].coordinates[2];
		
	delta = new Vector3f( 0.0f, +height/2.0f, -0.2f*width/2.0f ) ;
	vv[4] = new Vector3f( localOrigin );
	vv[4].add( delta );
	v[4] = new Vec3f();
	v[4].value[0] = vv[4].coordinates[0];
	v[4].value[1] = vv[4].coordinates[1];
	v[4].value[2] = vv[4].coordinates[2];

	vv[10] = new Vector3f( vv[4] );
	vv[10].coordinates[1] = -vv[10].coordinates[1];
	v[10] = new Vec3f();
	v[10].value[0] = vv[10].coordinates[0];
	v[10].value[1] = vv[10].coordinates[1];
	v[10].value[2] = vv[10].coordinates[2];
		
	delta = new Vector3f( 0.0f, +0.8f*height/2.0f, -0.6f*width/2.0f ) ;
	vv[5] = new Vector3f( localOrigin );
	vv[5].add( delta );
	v[5] = new Vec3f();
	v[5].value[0] = vv[5].coordinates[0];
	v[5].value[1] = vv[5].coordinates[1];
	v[5].value[2] = vv[5].coordinates[2];

	vv[9] = new Vector3f( vv[5] );
	vv[9].coordinates[1] = -vv[9].coordinates[1];
	v[9] = new Vec3f();
	v[9].value[0] = vv[9].coordinates[0];
	v[9].value[1] = vv[9].coordinates[1];
	v[9].value[2] = vv[9].coordinates[2];
		
	delta = new Vector3f( 0.0f, +0.3f*height/2.0f, -0.9f*width/2.0f ) ;
	vv[6] = new Vector3f( localOrigin );
	vv[6].add( delta );
	v[6] = new Vec3f();
	v[6].value[0] = vv[6].coordinates[0];
	v[6].value[1] = vv[6].coordinates[1];
	v[6].value[2] = vv[6].coordinates[2];
	
	vv[8] = new Vector3f( vv[6] );
	vv[8].coordinates[1] = -vv[8].coordinates[1];
	v[8] = new Vec3f();
	v[8].value[0] = vv[8].coordinates[0];
	v[8].value[1] = vv[8].coordinates[1];
	v[8].value[2] = vv[8].coordinates[2];
		
	delta = new Vector3f( 0.0f, 0.0f, -width/2.0f ) ;
	vv[7] = new Vector3f( localOrigin );
	vv[7].add( delta );
	vv[vert-1] = new Vector3f( vv[7] ); // Closed poligon => first and last vertices coincide.
	v[7] = new Vec3f();
	v[7].value[0] = vv[7].coordinates[0];
	v[7].value[1] = vv[7].coordinates[1];
	v[7].value[2] = vv[7].coordinates[2];

	final IntrinsicCrossSection poligon = new IntrinsicCrossSection( vert, v );
	poligon.setNormalsByVector( nNormalized );

	if( currentColor != null ) {
	    final Color3f[] colors = new Color3f[vert];
	    for( int i = 0; i < vert; i++ ) {
		colors[i] = new Color3f( currentColor );
	    }
	
	    poligon.setColors( colors );
	    poligon.setSideColor( colors[0] );
	
	    //	poligon.setSideColor( new Color3f( 1.0f, 1.0f, 0.0f ) );
	}
	final float time2 = System.currentTimeMillis();

	return poligon;
    }

    // Generate an INTRINSIC float face cross section.
    private static IntrinsicCrossSection  getIntrinsicDoubleFace( final CrossSectionStyle style, final int vert, final Vector3f normal ) {
	//
        // CONVENTION: The first axes is the tangent (versor: t), 
        // CONVENTION: the second one is the normal (versor: n)
        // CONVENTION: and the third one is the binormal (versor: b).
        //
        // Start with a reference rectangle with the longer side in the direction of the binormal
        // and the shorter side in the direction of the normal. 
        //
	final float time1 = System.currentTimeMillis();
	Vector3f nNormalized = null;
	final Vector3f[] delta = new Vector3f[vert];
	final Vec3f[] v = new Vec3f[vert];
	Vector3f n = null;
	final Vector3f[] vv = new Vector3f[vert];
	n = new Vector3f( 1.0f, 0.0f, 0.0f );
	nNormalized = new Vector3f( n ); // Save it.
	
	// Shape specifics come into play now.
	// Need a rectangle of semi-diagonal = sqrt(width**2 + height**2).
	final Vector3f localOrigin = new Vector3f( 0.0f, 0.0f, 0.0f );
	float width, height;
	width = style.getDiameters()[0];
	height = style.getDiameters()[1];
	Color3f currentColor = null;
	if( style.getVertexColors()[0] != null ) {
	    currentColor = style.getVertexColors()[0];
	}
	delta[0] = new Vector3f( 0.0f, 0.0f, width/2.0f ); // Perpendicular to t.
	final float s = (float)Math.sqrt( Math.pow(width, 2.0d) + Math.pow(height, 2.0d) );

	vv[0] = new Vector3f( localOrigin );
	vv[0].add( delta[0] );
	vv[vert-1] = vv[0];

	v[0] = new Vec3f();
	v[0].value[0] = vv[0].coordinates[0];
	v[0].value[1] = vv[0].coordinates[1];
	v[0].value[2] = vv[0].coordinates[2];
	v[vert-1] = v[0];
	
	// Reuse delta[0]
	delta[0].negate();
	vv[1] = new Vector3f( localOrigin );
	vv[1].add( delta[0] );
	v[1] = new Vec3f();
	v[1].value[0] = vv[1].coordinates[0];
	v[1].value[1] = vv[1].coordinates[1];
	v[1].value[2] = vv[1].coordinates[2];
		
	final IntrinsicCrossSection poligon = new IntrinsicCrossSection( vert, v );
	poligon.setNormalsByVector( nNormalized );

	if( currentColor != null ) {
	    final Color3f[] colors = new Color3f[vert];
	    colors[0] = colors[2] = new Color3f( 0.6f, 0.6f, 0.6f );
	    colors[1] = new Color3f( currentColor );
	
	    poligon.setColors( colors );
	    poligon.setSideColor( colors[0] );
	    //	poligon.setSideColor( new Color3f( 1.0f, 1.0f, 0.0f ) );
	}

	final float time2 = System.currentTimeMillis();
	////debug.println( 3, "Generating the cross section took: " + (time2-time1) + "ms" );

	return poligon;
    }

    // Generate an INTRINSIC single face cross section. (Open polyline)
    private static IntrinsicCrossSection  getIntrinsicSingleFace( final CrossSectionStyle style, final int vert, final Vector3f normal ) {
	//
        // CONVENTION: The first axes is the tangent (versor: t), 
        // CONVENTION: the second one is the normal (versor: n)
        // CONVENTION: and the third one is the binormal (versor: b).
        //
        // Start with a reference rectangle with the longer side in the direction of the binormal
        // and the shorter side in the direction of the normal. 
        //
	final float time1 = System.currentTimeMillis();
	Vector3f nNormalized = null;
	final Vector3f[] delta = new Vector3f[vert];
	final Vec3f[] v = new Vec3f[vert];
	Vector3f n = null;
	final Vector3f[] vv = new Vector3f[vert];
	n = new Vector3f( 1.0f, 0.0f, 0.0f );
	nNormalized = new Vector3f( n ); // Save it.
	
	// Shape specifics come into play now.
	// Need a rectangle of semi-diagonal = sqrt(width**2 + height**2).
	final Vector3f localOrigin = new Vector3f( 0.0f, 0.0f, 0.0f );
	float width, height;
	width = style.getDiameters()[0];
	height = style.getDiameters()[1];
	Color3f currentColor = null;
	if( style.getVertexColors() != null ) {
	    currentColor = style.getVertexColors()[0];
	}
	delta[0] = new Vector3f( 0.0f, 0.0f, width/2.0f ); // Perpendicular to t.
	final float s = (float)Math.sqrt(Math.pow( width, 2.0d) + Math.pow(height, 2.0d) );

	vv[0] = new Vector3f( localOrigin );
	vv[0].add( delta[0] );
	v[0] = new Vec3f();
	v[0].value[0] = vv[0].coordinates[0];
	v[0].value[1] = vv[0].coordinates[1];
	v[0].value[2] = vv[0].coordinates[2];
	
	// Reuse delta[0]
	delta[0].negate();
	vv[1] = new Vector3f( localOrigin );
	vv[1].add( delta[0] );
	v[1] = new Vec3f();
	v[1].value[0] = vv[1].coordinates[0];
	v[1].value[1] = vv[1].coordinates[1];
	v[1].value[2] = vv[1].coordinates[2];
		
	final IntrinsicCrossSection poligon = new IntrinsicCrossSection( vert, v );
	poligon.setNormalsByVector( nNormalized );

	if( currentColor != null ) {
	final Color3f[] colors = new Color3f[vert];
	colors[1] = colors[0] = new Color3f( currentColor );
		
	poligon.setColors( colors );
	poligon.setSideColor( colors[0] );
	
	//	poligon.setSideColor( new Color3f( 1.0f, 1.0f, 0.0f ) );
	}
	final float time2 = System.currentTimeMillis();
	//debug.println( 3, "Generating the cross section took: " + (time2-time1) + "ms" );

	return poligon;
    }

}

