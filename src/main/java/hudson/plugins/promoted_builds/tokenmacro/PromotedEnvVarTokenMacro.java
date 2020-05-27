/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.promoted_builds.tokenmacro;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.Promotion;
import java.io.IOException;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.impl.EnvironmentVariableMacro;

/**
 * Retrieves an environment variable from the {@link Promotion} build.
 * The {@link Promotion} build is being determined by the current {@link Executor}, 
 * so the macro is tolerant against context switches when the variable is being
 * resolved against the parent build instead of the {@link Promotion}.
 * In other cases the macro will behave similarly to {@link EnvironmentVariableMacro} from
 * Token Macro plugin.
 * @author Oleg Nenashev
 */
@Extension(optional = true)
public class PromotedEnvVarTokenMacro extends DataBoundTokenMacro {

    @Parameter(required=true)
    public String var = "";

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName) 
            throws MacroEvaluationException, IOException, InterruptedException {
        
        Executor currentExecutor = Executor.currentExecutor();
        if (currentExecutor == null) {
            return null;
        }
        
        Queue.Executable executable = currentExecutor.getCurrentExecutable();
        if (!(executable instanceof Promotion)) {
            return ""; // Nothing to do if it is not promotion
        }
        
        Map<String, String> env = ((Promotion)executable).getEnvironment(listener);
        if(env.containsKey(var)){
            return env.get(var);
        }
        return "";
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("PROMOTION_ENV");
    }
    
}
