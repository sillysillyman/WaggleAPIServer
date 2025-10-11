package io.waggle.waggleapiserver.domain.project.repository

import io.waggle.waggleapiserver.domain.project.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long>
