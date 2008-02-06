
WWW_DIR=/home/wistrand/knopflerfish.org/www
OUT_DIR=/home/wistrand/knopflerfish.org/www/snapshots
REPO=https://www.knopflerfish.org/svn/knopflerfish.org/trunk

TAG         = trunk
CURRENT_LINK=current_${TAG}
BUILD_KIND  = "snapshot build"

ANT	= /usr/pkg/bin/ant
AWK     = /usr/bin/awk
CAT     = /bin/cat
CP      = /bin/cp 
GREP    = /usr/bin/grep
LN      = /bin/ln
MKDIR   = /bin/mkdir
RM      = /bin/rm
SED     = /usr/bin/sed
SVN     = /usr/pkg/bin/svn

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
VERSION := ${REL_VERSION}.snapshot_${REVISION}

# Ant with build properties set.
ANTv = ${ANT} -Dversion=${VERSION} \
              -Dsvn.tag=${TAG} \
              -Dbuild.kind=${BUILD_KIND}



.PHONY: help all  clean version checkout update status
.PHONY: build distrib
.PHONY: copyout outindex clean_snapshots
.PHONY: obr_export obr obr_install


help:
	@echo "update       clean and update"
	@echo "all          builds distribution, tests it and publishes it."

all: version $(TAG)/ant/lib/bcel-5.1.jar distrib copyout clean_snapshots outindex

version:
	@echo "Current Knopflerfish version is: ${REL_VERSION}"
	@echo "Current repository version is: ${REVISION}"
	@echo "Current build version is: ${VERSION}"

obr_export:
	(cd ${TAG} && ${ANTv} obr obr_index) && \
	$(CP) -r ${TAG}/osgi/jars /home/wistrand/knopflerfish.org/www/repo
	$(CP) -r ${TAG}/repo/*.xml /home/wistrand/knopflerfish.org/www/repo
	$(CP) -r ${TAG}/repo/*.html /home/wistrand/knopflerfish.org/www/repo
	$(CP) -r ${TAG}/htdocs/css/repository.xsl /home/wistrand/knopflerfish.org/www/css



obr:
	(cd ${TAG} && ${ANTv} -Dobr.basedir=/home/wistrand/knopflerfish.org/www/repo/jars obr obr_index)

obr_install:
	$(CP) -r ${TAG}/repo ${WWW_DIR}

$(TAG)/ant/lib/bcel-5.1.jar:
	(cd ${TAG}/osgi && ${ANT} install_bcel)

checkout:
	${SVN} co ${REPO}

update:
	(cd ${TAG} && ${ANT} clean)
	(cd ${TAG} && ${SVN} update)

status:
	(cd ${TAG} && ${SVN} status)

build:
	(cd ${TAG} && ${ANTv} all)

distrib:
	(cd ${TAG} && ${ANTv} distrib)

clean:
	$(RM) -rf ${TAG}


CH_LOG_SRC=${WWW_DIR}/../ChangeLog_${TAG}
CH_LOG_DEST=${OUT_DIR}/${VERSION}/ChangeLog.txt

copyout:
	@$(MKDIR) -p ${OUT_DIR}
	$(CP) -r ${TAG}/htdocs/releases/${VERSION} ${OUT_DIR}
	@[ -r ${CH_LOG_SRC} ] && $(CP) -p $(CH_LOG_SRC) $(CH_LOG_DEST)

# Remove all but the 10 last BUILDs from ${OUT_DIR}
clean_snapshots:
	@$(MKDIR) -p ${OUT_DIR}
	@(cd ${OUT_DIR}; \
	  BUILD_CNT=0; \
	  for FF in `ls -1ct`; do \
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

