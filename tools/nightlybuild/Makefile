
WWW_DIR=/home/wistrand/knopflerfish.org/www
OUT_DIR=/home/wistrand/knopflerfish.org/www/snapshots
REPO=https://www.knopflerfish.org/svn/knopflerfish.org/trunk

TAG     = trunk

CURRENT_LINK=current_${TAG}

CAT     = /bin/cat
CP      = /bin/cp 
SVN     = /usr/pkg/bin/svn
ANT     = /home/polli/apache-ant-1.6.2/bin/ant
#ANT	= /usr/pkg/bin/ant
AWK     = /usr/bin/awk
GREP    = /usr/bin/grep
SED     = /usr/bin/sed
RM      = /bin/rm
LN      = /bin/ln
JAVA    = java
#JAVA    = /usr/pkg/java/sun-1.3.1/bin/java
#JAVA_HOME = /usr/pkg/java/sun-1.3.1

DATE := $(shell /bin/date '+%Y-%m-%d %H:%M')
# The current (upcomming) Knopflerfish release version
#REL_VERSION = 2.0.4
REL_VERSION := $(shell cd ${TAG} && ${ANT} version | ${GREP} "Version: " | ${SED} -e 's/.*Version: //g')

# The current SVN revision number
REVISION := $(shell ${SVN} info ${REPO} | ${GREP} 'Revision: ' | ${SED} -e 's/Revision: //')

# The version of this nightly build
VERSION := ${REL_VERSION}_${REVISION}

.PHONY: help all obr_export obr obr_install checkout update status build
.PHONY: distrib distrib_test clean run_tests run_tests_pre run_tests_post 
.PHONY: run_tests1 run_tests1_pa run_tests2 copyout outindex version
.PHONY: relindex clean_snapshots

help:
	@echo "update     clean and update"
	@echo "all        builds distribution, tests it and publishes it."
	@echo "run_tests  run regression tests"

all: version distrib distrib_tests copyout relindex  clean_snapshots outindex run_tests

version:
	@echo "Current Knopflerfish version is: ${REL_VERSION}"
	@echo "Current repository version is: ${REVISION}"
	@echo "Current build version is: ${VERSION}"

obr_export:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} obr obr_index) && \
	cp -r ${TAG}/osgi/jars /home/wistrand/knopflerfish.org/www/repo
	cp -r ${TAG}/repo/*.xml /home/wistrand/knopflerfish.org/www/repo
	cp -r ${TAG}/repo/*.html /home/wistrand/knopflerfish.org/www/repo
	cp -r ${TAG}/htdocs/css/repository.xsl /home/wistrand/knopflerfish.org/www/css



obr:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} -Dobr.basedir=/home/wistrand/knopflerfish.org/www/repo/jars obr obr_index)

obr_install:
	cp -r ${TAG}/repo ${WWW_DIR}

checkout:
	${SVN} co ${REPO}

update:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} clean)
	(cd ${TAG} && ${SVN} update)

status:
	(cd ${TAG} && ${SVN} status)

build:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} all)

distrib:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} distrib)

distrib_tests:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} distrib_tests)

clean:
	rm -rf ${TAG}

run_tests:
	${MAKE} run_tests_pre
	${MAKE} run_tests1
	${MAKE} run_tests2
	${MAKE} run_tests_post


run_tests_pre:
	mkdir -p ${TAG}/osgi/junit_grunt

run_tests_post:
	cp -r \
		${TAG}/osgi/junit_grunt \
		${OUT_DIR}
	cp \
		kf_junit_index_style.xsl \
		${OUT_DIR}/junit_grunt/junit_index_style.xsl

run_tests1:
	(\
	cd ${TAG}/osgi && \
	${JAVA} -jar framework.jar -xargs init-tests.xargs \
		> junit_grunt/out.txt 2>&1 \
	)

run_tests1_pa:
	(\
	cd ${TAG}/osgi && \
	${JAVA} -jar framework.jar -xargs init-tests_pa.xargs \
		>> junit_grunt/out.txt 2>&1 \
	)


run_tests2:
	(\
	cd ${TAG}/osgi && \
	${JAVA} -jar framework.jar -init \
	-xargs bundles_test/regression_tests/restart_test/test-restart1.xargs \
		>> junit_grunt/out.txt 2>&1 \
	)
	(\
	cd ${TAG}/osgi && \
	${JAVA} -jar framework.jar  \
	-xargs bundles_test/regression_tests/restart_test/test-restart2.xargs \
		>> junit_grunt/out.txt 2>&1 \
	)

CH_LOG_SRC=${WWW_DIR}/../ChangeLog_${TAG}
CH_LOG_DEST=${OUT_DIR}/${VERSION}/ChangeLog.txt

copyout:
	@mkdir -p ${OUT_DIR}
	${CP} -r ${TAG}/htdocs/releases/${VERSION} ${OUT_DIR}
	@[ -r ${CH_LOG_SRC} ] && ${CP} -p ${CH_LOG_SRC} ${CH_LOG_DEST}

relindex:
	cat rel_index.html.in | \
	  sed -e "s/@VERSION@/${VERSION}/g" \
              -e "s/@TAG@/${TAG}/g" \
              -e "s/@DATE@/${DATE}/g" \
	  > ${OUT_DIR}/${VERSION}/index.html

# Remove all but the 10 last BUILDs for each release from ${OUT_DIR}
clean_snapshots:
	@mkdir -p ${OUT_DIR}
	@(cd ${OUT_DIR}; \
	  BUILD_CNT=0; \
	  for FF in `ls -1dcrt ${REL_VERSION}_*`; do \
	    if [ -d $$FF ]; then \
	      BUILD_CNT=`expr $$BUILD_CNT + 1`; \
	      if [ $$BUILD_CNT -gt 10 ]; then \
	        ${RM} -rf $$FF; \
	      fi; \
	    fi; \
	  done)

outindex:
	@${RM} -f ${OUT_DIR}/${CURRENT_LINK}
	@${CAT} snap_index.html.pre > ${OUT_DIR}/index.html
	@echo "The current build on ${TAG} is <a href=\"${CURRENT_LINK}\">${VERSION}</a>.<p/>" >> ${OUT_DIR}/index.html
	@echo "<h3>All available snapshot builds</h3>" >> ${OUT_DIR}/index.html
	@echo "<p>" >> ${OUT_DIR}/index.html
	@(cd ${OUT_DIR}; \
	  for FF in `ls -1ct`; do \
	    if [ -d $$FF ]; then \
	      echo "<a href=\"$$FF\">$$FF</a><br/>" >> ${OUT_DIR}/index.html; \
	    fi; \
	  done)
	@${CAT} snap_index.html.post >> ${OUT_DIR}/index.html
	@(cd ${OUT_DIR} && ${LN} -s ${VERSION} ${CURRENT_LINK})

