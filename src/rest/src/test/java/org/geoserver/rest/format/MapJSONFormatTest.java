package org.geoserver.rest.format;

import static org.junit.Assert.assertThat;
import static org.geoserver.test.JsonMatcher.jsonEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MapJSONFormatTest {

    private MapJSONFormat mapJSONFormat;
    
    public @Rule TemporaryFolder temp = new TemporaryFolder();
    
    @Before
    public void setUp() {
        mapJSONFormat = new MapJSONFormat();
    }
    
    String toJson(Object obj) throws Exception {
        File f = temp.newFile();
        try (FileOutputStream of = new FileOutputStream(f)) {
            mapJSONFormat.write(obj, of);
        }
        byte[] encoded = Files.readAllBytes(f.toPath());
        return new String(encoded, Charset.defaultCharset()); // MapJSONFormat doesn't currently support specifying encoding so use default
    }
    
    @Test
    public void testBooleanResultTrue() throws Exception {
        assertThat(toJson(true), jsonEquals("true"));
    }

    @Test
    public void testBooleanResultFalse() throws Exception {
        assertThat(toJson(false), jsonEquals("false"));
    }

    @Test
    public void testNullResult() throws Exception {
        assertThat(toJson(null), jsonEquals("null"));
    }

    @Test
    public void testArray() throws Exception {
        assertThat(toJson(Arrays.asList(1, 2, 3)), jsonEquals("[1,2,3]"));
    }

    @Test
    public void testMap() throws Exception {
        assertThat(toJson(Collections.singletonMap("foo", "bar")), jsonEquals("{\"foo\":\"bar\"}"));
    }

    @Test
    public void testMapWithNullValue() throws Exception {
        assertThat(toJson(Collections.singletonMap("foo", null)), jsonEquals("{\"foo\":null}"));
    }

    @Test
    public void testNested() throws Exception {
        Map<String, Object> input = new HashMap<String, Object>();

        List<Object> list = Arrays.<Object> asList("quux", true, 7, null);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("fleem", "morx");
        map.put("list", list);

        input.put("map", map);

        assertThat(toJson(input), jsonEquals("{\"map\":{\"fleem\":\"morx\", \"list\":[\"quux\", true, 7, null]}}"));
    }
}
