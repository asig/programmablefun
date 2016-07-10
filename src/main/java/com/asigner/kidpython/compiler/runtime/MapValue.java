// Copyright 2016 Andreas Signer. All rights reserved.

package com.asigner.kidpython.compiler.runtime;

import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.asigner.kidpython.compiler.runtime.Value.Type.MAP;
import static java.util.stream.Collectors.joining;

public class MapValue extends Value {
    private final Map<Value, Value> mapVal;

    public MapValue(Map<Value, Value> val) {
        super(MAP);
        this.mapVal = val;
    }

    @Override
    public Iterator<? extends Value> asIterator() {
        return mapVal.values().iterator();
    }

    public List<Value> asList() {
            return mapVal.values().stream().collect(Collectors.toList());
    }

    public Map<Value, Value> asMap() {
        return mapVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapValue mapValue = (MapValue) o;

        if (this.mapVal.size() != mapValue.mapVal.size()) {
            return false;
        }
        for (Value key : this.mapVal.keySet()) {
            if (!Objects.equals(this.mapVal.get(key), mapValue.mapVal.get(key))) {
                return false;
            }
        }
        return true;
    }


    @Override
    public String toString() {
        return "MapValue{" + getType().toString() + ":" + mapVal + "}";
    }

}
