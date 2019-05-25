/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.ci.service.impl;

import com.wl4g.devops.ci.config.DeployProperties;
import com.wl4g.devops.ci.service.DependencyService;
import com.wl4g.devops.ci.utils.SSHTools;
import com.wl4g.devops.ci.utils.GitUtils;
import com.wl4g.devops.common.bean.ci.Dependency;
import com.wl4g.devops.common.bean.ci.Project;
import com.wl4g.devops.dao.ci.DependencyDao;
import com.wl4g.devops.dao.ci.ProjectDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author vjay
 * @date 2019-05-22 11:39:00
 */
@Service
public class DependencyServiceImpl implements DependencyService {

	@Autowired
	private DependencyDao dependencyDao;

	@Autowired
	private DeployProperties devConfig;

	@Autowired
	private ProjectDao projectDao;

	@Override
	public void build(Dependency dependency, String branch) throws Exception {

		Integer projectId = dependency.getProjectId();

		List<Dependency> dependencies = dependencyDao.getParentsByProjectId(projectId);
		if (dependencies != null && dependencies.size() > 0) {
			for (Dependency dep : dependencies) {
				String br = dep.getParentBranch();
				build(new Dependency(dep.getParentId()), StringUtils.isBlank(br) ? branch : br);
			}
		}

		// TODO build
		Project project = projectDao.selectByPrimaryKey(projectId);
		String path = devConfig.getGitBasePath() + "/" + project.getProjectName();
		if (checkGitPahtExist(path)) {
			GitUtils.checkout(path, branch);
		} else {
			GitUtils.clone(project.getGitUrl(), path, branch);
		}
		// install
		install(path);

	}

	public boolean checkGitPahtExist(String path) throws Exception {
		File file = new File(path + "/.git");
		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * build (maven)
	 */
	public String install(String path) throws Exception {
		String command = "mvn -f " + path + "/pom.xml clean install -Dmaven.test.skip=true";
		return SSHTools.runLocal(command);
	}

}