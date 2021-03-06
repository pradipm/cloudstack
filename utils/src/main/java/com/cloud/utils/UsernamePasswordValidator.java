//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernamePasswordValidator {
    private Pattern usernamePattern;
    private Pattern passwordPattern;
    private Matcher matcher;

    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9@._-]{2,63}$";
    private static final String PASSWORD_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9@#+=._-]{2,31}$";

    public UsernamePasswordValidator() {
        usernamePattern = Pattern.compile(USERNAME_PATTERN);
        passwordPattern = Pattern.compile(PASSWORD_PATTERN);

    }

    public boolean validateUsername(final String username) {
        matcher = usernamePattern.matcher(username);
        return matcher.matches();
    }

    public boolean validatePassword(final String password) {
        matcher = passwordPattern.matcher(password);
        return matcher.matches();
    }

}
