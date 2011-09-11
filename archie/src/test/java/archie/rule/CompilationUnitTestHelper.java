/*
 Copyright 2011 Frode Carlsen

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package archie.rule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.junit.Assert;

public class CompilationUnitTestHelper {

    @SuppressWarnings("unused")
    private final IJavaProject javaProject;

    public CompilationUnitTestHelper() throws Exception {
        javaProject = createJavaProject("P");
    }

    CompilationUnit createCompilationUnit(String fullyQualifiedClassName, String content) throws CoreException {
        String path = fullyQualifiedClassName.replace('.', '/') + ".java";
        String fullPath = "P/src/" + path;
        createFolder(new Path(fullPath.substring(0, fullPath.lastIndexOf('/'))));
        createFile(fullPath, content);
        ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(getFile(fullPath));

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(compilationUnit);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(true);
        parser.setResolveBindings(true);

        return (CompilationUnit) parser.createAST(null);
    }

    private static IJavaProject createJavaProject(String projectName) throws Exception {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        IProject project = root.getProject(projectName);
        project.create(null);
        project.open(null);

        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        project.setDescription(description, null);

        IJavaProject javaProject = JavaCore.create(project);

        IClasspathEntry[] buildPath = {
                JavaCore.newSourceEntry(javaProject.getPath().append("src")),
                JavaRuntime.getDefaultJREContainerEntry()
        };
        javaProject.setRawClasspath(buildPath, project.getFullPath().append("bin"), null);
        javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);

        // create source folder
        IFolder folder = project.getFolder("src");
        folder.create(true, true, null);
        IPackageFragmentRoot srcFolder = javaProject.getPackageFragmentRoot(folder);
        Assert.assertTrue(srcFolder.exists()); // resource exists and is on build path

        return javaProject;

    }

    private static IFile getFile(String path) {
        return getWorkspaceRoot().getFile(new Path(path));
    }

    private static IFile createFile(String path, String content) {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        try {
            IFile file = getFile(path);
            file.create(inputStream, true, null);
            inputStream.close();
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private static IFolder createFolder(IPath path) throws CoreException {
        final IFolder folder = getWorkspaceRoot().getFolder(path);
        if (folder.exists()) {
            return folder;
        }
        getWorkspace().run(new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                IContainer parent = folder.getParent();
                if (parent instanceof IFolder && !parent.exists()) {
                    createFolder(parent.getFullPath());
                }
                folder.create(true, true, null);
            }
        }, null);

        return folder;
    }

    private static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    private static IWorkspaceRoot getWorkspaceRoot() {
        return getWorkspace().getRoot();
    }

}
