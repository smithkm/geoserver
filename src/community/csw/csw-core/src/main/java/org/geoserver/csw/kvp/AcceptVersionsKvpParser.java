/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;


import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.Ows10Factory;

import org.eclipse.emf.ecore.EObject;

/**
 * Parses a kvp of the form "acceptVersions=version1,version2,...,versionN" into
 * an instance of {@link net.opengis.ows.v1_1_0.AcceptVersionsType}.
 *
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AcceptVersionsKvpParser extends org.geoserver.ows.kvp.AcceptVersionsKvpParser {
    public AcceptVersionsKvpParser() {
        super(AcceptVersionsType.class);
        setService("csw");
    }

    @Override
    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createAcceptVersionsType();
    }
}
