package org.knopflerfish.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;

public class BundleRevisionsImpl extends BundleReferenceImpl implements BundleRevisions {

  private Vector<BundleGeneration> generations;

  BundleRevisionsImpl(Vector<BundleGeneration> generations) {
    super(generations.get(0).bundle);
    this.generations = generations;
  }

  public List<BundleRevision> getRevisions() {
    synchronized (generations) {
      List<BundleRevision> res = new ArrayList<BundleRevision>(generations.size());
      for (BundleGeneration bg : generations) {
        if (!bg.isUninstalled()) {
          res.add(bg.getBundleRevision());
        }
      }
      return res;
    }
  }

}
