
TAG         = trunk
TAGs        = $(subst /,_,$(TAG))
CURRENT_LINK=current_${TAGs}
BUILD_KIND  = "snapshot build"

KF_HOME=/home/knopflerfish
WWW_DIR=${KF_HOME}/www
WWW_PATH=snapshots_${TAGs}
OUT_DIR=${WWW_DIR}/${WWW_PATH}
OUT_URL=http://www.knopflerfish.org/${WWW_PATH}

CHANGE_LOG=${KF_HOME}/ChangeLog_${TAGs}
REPO=https://www.knopflerfish.org/svn/knopflerfish.org/${TAG}

# Build must be done using Java SE 5 or 6 due to '-target=jsr14'
JAVA      = java
export JAVA_HOME = /usr

#JAVA      = /usr/local/j2sdk1.4/bin/java
#export JAVA_HOME = /usr/local/j2sdk1.4


#ANT	= /usr/bin/ant
ANT	= /home/knopflerfish/ant/apache-ant-1.9.5/bin/ant
AWK     = /usr/bin/awk
CAT     = /bin/cat
CP      = /bin/cp 
GREP    = /bin/grep
LN      = /bin/ln
MKDIR   = /bin/mkdir
RM      = /bin/rm
SED     = /bin/sed
SVN     = /usr/bin/svn
SVNVERSION = /usr/bin/svnversion

YEAR := $(shell /bin/date '+%Y')
DATE := $(shell /bin/date '+%Y-%m-%d %H:%M')
# The Major.Minor.Micro part of the current (upcomming) Knopflerfish
# release version.
# E.g., REL_VERSION = 2.0.4
REL_VERSION := $(shell cd ${TAGs} && ${ANT} version | \
                       ${GREP} "Version: " | \
                       ${SED} -e 's/.*Version: //g' \
                              -e 's/\.[0-9]*[-a-zA-Z_][-0-9aa-zA-Z_]*//')

# The current SVN revision number
# Suppress trailing 'M' since allways modfied due to the change log file.
REVISION := $(shell ${SVNVERSION} ${TAGs} | ${SED} -e 's/M//g')

# The version of this nightly build
VERSION := ${REL_VERSION}.snapshot_${TAGs}_${REVISION}

# Ant with build properties set.
ANTv = ${ANT} -Dversion=${VERSION} \
              -Dsvn.tag=${TAG} \
              -Dbuild.kind=${BUILD_KIND} \
	      -Dbase.url=${OUT_URL}


.PHONY: help all  clean version checkout update status
.PHONY: build distrib
.PHONY: copyout outindex clean_snapshots
.PHONY: obr_export obr obr_install


help:
	@echo "update       clean and update"
	@echo "all          builds distribution, tests it and publishes it."

all: version distrib mvnrepo copyout clean_snapshots outindex

version:
	@echo "Current Knopflerfish version is: ${REL_VERSION}"
	@echo "Current repository version is: ${REVISION}"
	@echo "Current build version is: ${VERSION}"

obr:
	(cd ${TAGs} && ${ANTv} obr bindex)

obr_install:
	$(CP) ${TAGs}/repo/* ${WWW_DIR}/repo
	${RM} -f ${WWW_DIR}/repo/jars
	${LN} -s ${WWW_DIR}/releases/${VERSION}/jars ${WWW_DIR}/repo/

checkout:
	${SVN} co ${REPO} ${TAGs}

update:
	(cd ${TAGs} && ${ANT} clean)
	-${RM} -f ${TAGs}/changelog.txt
	(cd ${TAGs} && ${SVN} update)
	[ -r ${CHANGE_LOG} ] && cp ${CHANGE_LOG} ${TAGs}/changelog.txt

status:
	(cd ${TAGs} && ${SVN} status)

build:
	(cd ${TAGs} && ${ANTv} all)

distrib:
	(cd ${TAGs} && ${ANTv} distrib)
	[ -r ${TAGs}/out/tmp/toMvn.xml ] && (cd ${TAGs} && ${ANT} -f out/tmp/toMvn.xml)

mvnrepo:
	[ -r ${TAGs}/out/tmp/toMvn.xml ] && (cd ${TAGs} && ${ANT} -f out/tmp/toMvn.xml)

clean:
	$(RM) -rf ${TAGs}

copyout:
	@$(MKDIR) -p ${OUT_DIR}
	-${RM} -rf ${OUT_DIR}/${VERSION}
	$(CP) -r ${TAGs}/out/distrib_${VERSION} ${OUT_DIR}/${VERSION}

# Remove all but the 10 last BUILDs on the current ${TAG} from ${OUT_DIR}
clean_snapshots:
	@$(MKDIR) -p ${OUT_DIR}
	@(cd ${OUT_DIR}; \
	  BUILD_CNT=0; \
	  for FF in `ls -1ctd *snapshot_${TAGs}*`; do \
	    if [ -d $$FF ]; then \
	      BUILD_CNT=`expr $$BUILD_CNT + 1`; \
	      if [ $$BUILD_CNT -gt 10 ]; then \
	        ${RM} -rf $$FF; \
	      fi; \
	    fi; \
	  done)

outindex:
	@${RM} -f ${OUT_DIR}/${CURRENT_LINK}
	@${CAT} ${TAGs}/tools/nightlybuild/snap_index.html.pre | \
            ${SED} -e "s/@DATE@/${DATE}/g" \
                   -e "s!@REPO@!${REPO}!g" \
                                                        > ${OUT_DIR}/index.html
	@echo "The current build on ${TAG} is <a href=">> ${OUT_DIR}/index.html
	@echo "\"${CURRENT_LINK}\">${VERSION}</a>.<p/>">> ${OUT_DIR}/index.html
	@echo "There is a dedicated maven repository "  >> ${OUT_DIR}/index.html
	@echo "for each snapshot build. This repository ">> ${OUT_DIR}/index.html
	@echo "will contain one version of each artifact.">> ${OUT_DIR}/index.html
	@echo "The repository URL for the current build is ">> ${OUT_DIR}/index.html
	@echo "<a href=\"http://www.knopflerfish.org/snapshots/current_trunk/maven2/\">http://www.knopflerfish.org/snapshots/current_trunk/maven2/</a>. ">> ${OUT_DIR}/index.html
	@echo "A more detailed description of the principles for the " >> ${OUT_DIR}/index.html
	@echo "Knopflerfish maven repository can be found ">> ${OUT_DIR}/index.html
	@echo "<a href=\"/maven.html\">here</a>.<p/>"  >> ${OUT_DIR}/index.html
	@echo "<h2 class=\"kf\">All available snapshot builds</h2>" >> ${OUT_DIR}/index.html
	@echo "Builds are listed in descending build " >> ${OUT_DIR}/index.html
	@echo "date order, i.e., most recent first."   >> ${OUT_DIR}/index.html
	@echo "<p>"                                    >> ${OUT_DIR}/index.html
	@(cd ${OUT_DIR}; \
	  for FF in `ls -1ct`; do \
	    if [ -d $$FF ]; then \
	      echo "<a href=\"$$FF\">$$FF</a> "  >> ${OUT_DIR}/index.html; \
	      echo " -  "  >> ${OUT_DIR}/index.html; \
	      echo "<a href=\"$$FF/maven2\">Maven2 repo</a><br/>"  >> ${OUT_DIR}/index.html; \
	    fi; \
	  done)
	@${CAT} ${TAGs}/tools/nightlybuild/snap_index.html.post | \
            ${SED} -e "s/@YEAR@/${YEAR}/g" \
                                                       >> ${OUT_DIR}/index.html
	@(cd ${OUT_DIR} && ${LN} -s ${VERSION} ${CURRENT_LINK})

