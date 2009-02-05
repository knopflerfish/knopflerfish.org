/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.component;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

public class ImmediateComponent extends Component {

  public ImmediateComponent(Config config, Dictionary overriddenProps) {
    super(config, overriddenProps);
  }

  public void satisfied() {
    if (!isActivated()) {
      activate();
    }
    registerService();
  }

  public void unsatisfied() {
    unregisterService();
    deactivate();
  }

  
  public Object getService(Bundle bundle, ServiceRegistration reg) {
    super.getService(bundle, reg);
    
    if (!isActivated()) {
      activate();
    }
    
    if (!isActivated()) { // todo read spec.
      unregisterService();
      return null;
    }
    
    return getInstance();
  }

  public void ungetService(Bundle bundle, ServiceRegistration reg, Object o) {
    super.ungetService(bundle, reg, o);
  }

}
