package eu.delving.test;

import org.junit.Assert;
import org.junit.Test;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.datum.Datum;
import org.osgeo.proj4j.proj.MercatorProjection;
import org.osgeo.proj4j.proj.Projection;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class PointToLatLongConverterTest {

    @Test
    public void testPointToLatLongConversion() {

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory crsFactory = new CRSFactory();

        CoordinateReferenceSystem source = crsFactory.createFromParameters("EPSG:32633", "+proj=utm +zone=33 +ellps=WGS84 +datum=WGS84 +units=m +no_defs");
        CoordinateReferenceSystem dest = crsFactory.createFromName("EPSG:4326"); // WGS84


        String stringPoint = "SRID=32633;POINT(272027,6895706)";

        int place1 = stringPoint.indexOf("(") + 1;
        int place2 = stringPoint.indexOf(")");

        String[] point = stringPoint.substring(place1, place2).split(",");

        ProjCoordinate p = new ProjCoordinate(Double.parseDouble(point[0]), Double.parseDouble(point[1]));

        CoordinateTransform trans = ctFactory.createTransform(source, dest);

        ProjCoordinate res = new ProjCoordinate();

        trans.transform(p, res);

        Assert.assertEquals(res.x, 10.627299098922382, 0);
        Assert.assertEquals(res.y, 62.12415112646997, 0);
    }
}
