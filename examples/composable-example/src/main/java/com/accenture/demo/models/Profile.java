/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

 package com.accenture.demo.models;

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.Map;

public class Profile {

    private Integer id;
    private String name;
    private String address;
    private String telephone;

    public static Profile create(int id, String name, String address, String telephone) {
        var profile = new Profile();
        profile.setId(id);
        profile.setName(name);
        profile.setAddress(address);
        profile.setTelephone(telephone);
        return profile;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        return SimpleMapper.getInstance().getMapper().readValue(this, Map.class);
    }
}
