/**
 * Copyright 2016 ECS Team, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ecsteam.cloudfoundry.srt;

import static org.springframework.session.data.redis.config.ConfigureRedisAction.NO_OP;

import javax.servlet.http.HttpSession;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SessionReplicationTestApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SessionReplicationTestApplication.class, args);
	}

	/*
	 * Used in WAR deployment to tell Tomcat to start Bean post-processing
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(SessionReplicationTestApplication.class);
	}

	/*
	 * Taken from https://github.com/spring-projects/spring-session/issues/124. The PCF redis tile is secured, so the
	 * CONFIG command is disabled. Used only in Spring Boot (JAR) deployment.
	 */
	@Bean
	@ConditionalOnClass(name = "org.springframework.session.data.redis.config.ConfigureRedisAction")
	public static ConfigureRedisAction configureRedisAction() {
		return NO_OP;
	}
}

/**
 * Shows an incrementing counter with instance index. When a request is received, it kills the current JVM one second
 * after returning the response, so that Cloud Foundry will un-stick the session and move to another instance. When
 * moving to another instance, if replication is set up properly, the counter should continue to increment.
 * 
 * @author Josh Ghiloni <jghiloni@ecsteam.com>
 *
 */
@RestController
class TestEndpoint {
	private final int instanceIndex;

	@Autowired
	public TestEndpoint(@Value("${vcap.application.instance_index:-1}") int instanceIdx) {
		this.instanceIndex = instanceIdx;
	}

	@RequestMapping("/")
	public ResponseObject get(HttpSession session) {
		ResponseObject object = new ResponseObject();

		object.setInstanceIndex(instanceIndex);

		Integer count = (Integer) session.getAttribute("count");
		if (count == null) {
			count = 0;
		} else {
			++count;
		}

		session.setAttribute("count", count);

		object.setCount((Integer) session.getAttribute("count"));

		// we have this here because CF does try to stick a session using the session cookie whenever possible.
		// by killing the instance, we force it to go to another one.
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				System.exit(0);
			} catch (Exception e) {
			}

		}).start();

		return object;
	}
}

@Data
class ResponseObject {
	private int instanceIndex;

	private int count;
}