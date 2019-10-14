package manifest.utils

import file.FileNameUtils
import force.ForceService
/**
 * Created by miy4ko on 7/14/17.
 */
class Flow {

    ForceService forceService

    List<String> _flows = []

    Flow(ForceService forceService) {
        this.forceService = forceService
    }

    def getFlows() {
        List<String> ids = []
        _flows = []
        forceService.toolConnection.query('SELECT ActiveVersionId FROM FlowDefinition').records.each {
            if (it.ActiveVersionId != null) {
                ids << it.ActiveVersionId.toString()
            }
        }
        ids.each {
            _flows << forceService.toolConnection.retrieve('Fullname', 'Flow', it).FullName[0].toString()
        }
        _flows
    }

    def getFlowDefinitions() {
        if (_flows == null) {
            getFlows()
        }
        List<String> res = []
        _flows.each {
            res << FileNameUtils.removeFilenameExtension(it, '-')
        }
        res
    }
}

