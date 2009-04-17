
WWW_DIR=/home/wistrand/knopflerfish.org/www
OUT_DIR=/home/wistrand/knopflerfish.org/www/snapshots/trunk
REPO=https://www.knopflerfish.org/svn/knopflerfish.org/trunk

VERSION = 1.3.2
TAG     = trunk
SUFFIX  = -trunk


CP      = /bin/cp -p
SVN     = /usr/pkg/bin/svn
ANT     = /home/wistrand/knopflerfish.org/build/ant
#ANT	= /usr/pkg/bin/ant
MAKE    = /usr/bin/make
AWK     = /usr/bin/awk
JAVA    = /usr/pkg/java/sun-1.3.1/bin/java

help:
	echo "all        build all"
	echo "run_tests  run regression tests"

all: 
	${MAKE} update && \
	${MAKE} rebuild && \
	${MAKE} distrib_tests && \
	${MAKE} copyout
	${MAKE} run_tests

obr_export:
	(cd ${TAG} && ${ANT} obr) && \
	cp -r ${TAG}/osgi/jars /home/wistrand/knopflerfish.org/www/repo
	cp -r ${TAG}/repo/*.xml /home/wistrand/knopflerfish.org/www/repo
	cp -r ${TAG}/htdocs/css/repository.xsl /home/wistrand/knopflerfish.org/www/css


obr:
	(cd ${TAG} && ${ANT} -Dobr.basedir=/home/wistrand/knopflerfish.org/www/repo/jars obr obr_index)

obr_install:
	cp -r ${TAG}/repo ${WWW_DIR}

checkout:
	${SVN} co ${REPO}

update:
	${SVN} update ${TAG}

status:
	${SVN} status ${TAG}

rebuild:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} clean all)
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} distrib)

distrib_tests:
	(cd ${TAG} && ${ANT} -Dversion=${VERSION} distrib_tests)

build:
	(cd ${TAG} && ${ANT} all)

distrib:
	(cd ${TAG} && ${ANT} distrib)

public:
	cp $TAG/*.jar TAG/*.zip \
	$OUT_DIR

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

run_tests1:
	(\
	cd ${TAG}/osgi && \
	${JAVA} -jar framework.jar -xargs init-tests.xargs \
		> junit_grunt/out.txt 2>&1 \
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



copyout:
	@mkdir -p ${OUT_DIR}
	@${CP} ${TAG}/knopflerfish_bin_osgi_${VERSION}.jar \
	   ${OUT_DIR}/knopflerfish_bin_osgi_${VERSION}${SUFFIX}.jar	

	@${CP} ${TAG}/knopflerfish_fullbin_osgi_${VERSION}.jar \
	${OUT_DIR}/knopflerfish_fullbin_osgi_${VERSION}${SUFFIX}.jar

	@${CP} ${TAG}/knopflerfish_opt_${VERSION}.jar \
	${OUT_DIR}/knopflerfish_opt_${VERSION}${SUFFIX}.jar

	@${CP} ${TAG}/knopflerfish_osgi_${VERSION}.jar \
	${OUT_DIR}/knopflerfish_osgi_${VERSION}${SUFFIX}.jar

	@${CP} ${TAG}/knopflerfish_osgi_tiny_${VERSION}.zip \
	${OUT_DIR}/knopflerfish_osgi_tiny_${VERSION}${SUFFIX}.zip

	@${CP} ${TAG}/knopflerfish_osgi_tests_${VERSION}.zip \
	${OUT_DIR}/knopflerfish_osgi_tests_${VERSION}${SUFFIX}.zip

	@(cd ${OUT_DIR} && /bin/ls -l *.jar *.zip) \
	| ${AWK} -f outlist.awk \
	> ${OUT_DIR}/index.html

	@${CP} -r ${TAG}/osgi/jars \
	${OUT_DIR}/jars


outindex:
	@(cd ${OUT_DIR} && /bin/ls -l *.jar *.zip) \
	| ${AWK} -f outlist.awk \
	> ${OUT_DIR}/index.html


