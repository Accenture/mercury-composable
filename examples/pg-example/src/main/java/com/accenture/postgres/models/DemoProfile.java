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

package com.accenture.postgres.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

@Table(name = "demo_profile")
public class DemoProfile {

    @Id
    public String id;
    public String name;
    public String address;
    public Date created;

    public DemoProfile create(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.created = new Date();
        return this;
    }

    public String toString() {
        return "id=" + id +", name="+name+", address="+address+", created="+created;
    }
}
